using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Windows.Forms;
using System.Runtime.InteropServices;

namespace agun_server
{
    public partial class MainForm : Form
    {
        [DllImport("user32.dll")]
        // https://stackoverflow.com/questions/8272681/how-can-i-simulate-a-mouse-click-at-a-certain-position-on-the-screen
        private static extern void mouse_event(int dwFlags, int dx, int dy, int cButtons, int dwExtraInfo);
        private readonly int MOUSEEVENTF_MOVE = 0x01;
        private readonly int MOUSEEVENTF_LEFTDOWN = 0x02;
        private readonly int MOUSEEVENTF_LEFTUP = 0x04;
        private readonly int MOUSEEVENTF_RIGHTDOWN = 0x08;
        private readonly int MOUSEEVENTF_RIGHTUP = 0x10;

        private readonly int MAIN_PORT = 20415;
        private readonly int REPLY_PORT = 20416;

        private bool left_down = false;
        private bool right_down = false;
        private bool w_down = false;
        private bool s_down = false;
        private bool tab_down = false;
        private bool hold_down = false;

        Thread mainThread;
        Thread replyThread;
        public MainForm()
        {
            InitializeComponent();
        }

        private void MainForm_Load(object sender, EventArgs e)
        {
            lblPort.Text = "Port : " + MAIN_PORT;

            // main thread. receive state of mouse and keyboard
            mainThread = new Thread(() =>
            {
                UdpClient client = new UdpClient(MAIN_PORT);
                IPEndPoint ipep = null;
                Keyboard keyboard = new Keyboard();
                
                while (true)
                {
                    byte[] data = client.Receive(ref ipep); // 0~3 : x, 4~7 : y, 8 : button status
                    if (BitConverter.IsLittleEndian)
                    {
                        Array.Reverse(data, 0, 4);
                        Array.Reverse(data, 4, 4);
                    }
                    int x = BitConverter.ToInt32(data, 0);
                    int y = BitConverter.ToInt32(data, 4);

                    hold_down = (data[8] & 0x20) > 0;

                    if (!hold_down)
                    {
                        // Check mouse move delta(variance)
                        mouse_event(MOUSEEVENTF_MOVE, x, y, 0, 0);
                        // Check mouse left button
                        if (left_down != (left_down = ((data[8] & 0x01) > 0)))
                        {
                            mouse_event(left_down ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP, Cursor.Position.X, Cursor.Position.Y, 0, 0);
                        }
                        // Check mouse right button
                        if (right_down != (right_down = ((data[8] & 0x02) > 0)))
                        {
                            mouse_event(right_down ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP, Cursor.Position.X, Cursor.Position.Y, 0, 0);
                        }

                        // Check keyboard w button
                        if (w_down != (w_down = ((data[8] & 0x04) > 0)))
                        {
                            keyboard.Send(Keyboard.ScanCodeShort.KEY_W, w_down ? 0 : Keyboard.KEYEVENTF.KEYUP);
                        }
                        // Check keyboard s button
                        if (s_down != (s_down = ((data[8] & 0x08) > 0)))
                        {
                            keyboard.Send(Keyboard.ScanCodeShort.KEY_S, s_down ? 0 : Keyboard.KEYEVENTF.KEYUP);
                        }
                        // Check keyboard tab button
                        if (tab_down != (tab_down = ((data[8] & 0x10) > 0)))
                        {
                            keyboard.Send(Keyboard.ScanCodeShort.TAB, tab_down ? 0 : Keyboard.KEYEVENTF.KEYUP);
                        }
                    }
                }
            });
            mainThread.IsBackground = true;
            mainThread.Start();

            // reply thread. reply to broadcast packets. The sender will know server`s IP.
            replyThread = new Thread(() =>
            {
                UdpClient client = new UdpClient(REPLY_PORT);
                IPEndPoint ipep = null;

                while (true)
                {
                    byte[] data = client.Receive(ref ipep);
                    client.Send(data, 1, ipep); // reply just 1 byte
                }

            });
            replyThread.IsBackground = true;
            replyThread.Start();
        }
    }
}
