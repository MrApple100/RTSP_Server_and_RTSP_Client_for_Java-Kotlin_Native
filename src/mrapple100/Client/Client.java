package mrapple100.Client;

import mrapple100.Client.rtsp.widget.RtspSurfaceView;
import mrapple100.Server.CustomConnectCheckerRTSP;
import mrapple100.Server.rtspserver.RtspServerCamera1;
import mrapple100.utils.FrameSynchronizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.URI;

public class Client {
    static RtspServerCamera1 rtspServerCamera1;

    static RtspSurfaceView rtcpSurfaceView;
    //GUI
    //----
    JFrame f = new JFrame("mrapple100.Client.Client");
    JButton playButton = new JButton("Receive");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JPanel buttonPanel2 = new JPanel();
    JLabel statLabel1 = new JLabel();
    JLabel statLabel2 = new JLabel();
    JLabel statLabel3 = new JLabel();
    JLabel framePlace = new JLabel();

    JLabel frameAfterPlace = new JLabel();
    JLabel iplabel = new JLabel();
    JButton sendButton = new JButton("Send");



    ImageIcon icon;

    Timer timer; //timer used to receive data from the UDP socket


    //Server data
    static String RTSP_server_port;
    static String ServerHost;
    static String VideoFileName; //video file to request to the server
    static String url;

    InetAddress ServerIPAddr;




    FrameSynchronizer fsynch;

    //--------------------------
    //Constructor
    //--------------------------
    public Client() {

        //build GUI
        //--------------------------

        //Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
               // stopDecoders();
                System.exit(0);
            }
        });

        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(playButton);
        buttonPanel2.setLayout(new GridLayout(1,0));
        buttonPanel2.add(sendButton);

        playButton.addActionListener(new playButtonListener());
        sendButton.addActionListener(new sendButtonListener());


        //Statistics
        statLabel1.setText("Total Bytes Received: 0");
        statLabel2.setText("Packets Lost: 0");
        statLabel3.setText("Data Rate (bytes/sec): 0");

        //Image display label
        framePlace.setIcon(null);

        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(framePlace);
        mainPanel.add(frameAfterPlace);
        mainPanel.add(buttonPanel);
        mainPanel.add(buttonPanel2);

        // mainPanel.add(sendButton);
        mainPanel.add(statLabel1);
        mainPanel.add(statLabel2);
        mainPanel.add(statLabel3);
        mainPanel.add(iplabel);
        framePlace.setBounds(0,0,500,1000);
        frameAfterPlace.setBounds(800,0,500,1000);
        iplabel.setBounds(1000,900,380,50);
        buttonPanel.setBounds(200,900,380,50);
        buttonPanel2.setBounds(600,900,380,50);

        statLabel1.setBounds(500,950,380,20);
        statLabel2.setBounds(500,970,380,20);
        statLabel3.setBounds(500,990,380,20);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(1200,1200));
        f.setVisible(true);

        //init timer
        //--------------------------
       /* timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
*/
        //init RTCP packet sender
       // rtcpSender = new RtcpSender(400);

        //allocate enough memory for the buffer used to receive data from the server
       // buf = new byte[15000];

        //create the frame synchronizer
       // fsynch = new FrameSynchronizer(100);
    }

    //------------------------------------
    //main
    //------------------------------------
    public static void main(String argv[]) throws Exception {


        //Create a mrapple100.Client.Client object
        Client theClient = new Client();
        rtspServerCamera1 = new RtspServerCamera1(theClient.frameAfterPlace, new CustomConnectCheckerRTSP(), 1936);

        rtcpSurfaceView = new RtspSurfaceView(theClient.framePlace,rtspServerCamera1);

        //get server RTSP port and IP address from the command line
        //------------------
                RTSP_server_port = "1935";
        ServerHost = "192.168.19.208";//172.30.221.30
        theClient.ServerIPAddr = InetAddress.getByName(ServerHost);

        //get video filename to request:
        VideoFileName = "cast/1";


        url = "rtsp://"+ServerHost+":"+RTSP_server_port+"/"+VideoFileName;



        //Establish a TCP connection with the server to exchange RTSP messages
        //------------------
        /*System.out.println( theClient.ServerIPAddr.getAddress());
        System.out.println(theClient.ServerIPAddr.getHostName()+" "+RTSP_server_port);
*/
        /*theClient.RTSPsocket = new Socket(theClient.ServerIPAddr, RTSP_server_port);
        //Establish a UDP connection with the server to exchange RTCP control packets
        //------------------

        //Set input and output stream filters:
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSPsocket.getOutputStream()));
*/
        //init RTSP state:
      //  state = INIT;
    }


    //Handler for Play button
    //-----------------------
    class playButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            if (rtcpSurfaceView.isStarted()) {
                rtcpSurfaceView.stop();
            } else {
                URI uri = URI.create(url);
                System.out.println(uri);
                rtcpSurfaceView.init(uri, "", "", "rtsp-client-android");
              //  rtcpSurfaceView.debug = binding.cbDebug.isChecked
                rtcpSurfaceView.start(true, false);
            }
        }
    }

    //Handler for Send button
    //-----------------------
    class sendButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            if (!rtspServerCamera1.isStreaming()) {
                System.out.println(rtspServerCamera1.prepareVideo());
                if (  rtspServerCamera1.prepareVideo()) {
                    sendButton.setText("Server_is_started!");
                    rtspServerCamera1.startStream();
                    iplabel.setText(rtspServerCamera1.getEndPointConnection());
                    System.out.println(rtspServerCamera1.getEndPointConnection()+"");
                }
            } else {
                sendButton.setText("Stop_Server");
                rtspServerCamera1.stopStream();
                iplabel.setText("");
            }

        }
    }

    //------------------------------------
    //Handler for timer
    //------------------------------------
    /*class timerListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(buf, buf.length);

            try {
                //receive the DP from the socket, save time for stats
                RTPsocket.receive(rcvdp);

                double curTime = System.currentTimeMillis();
                statTotalPlayTime += curTime - statStartTime;
                statStartTime = curTime;

                //create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                int seqNb = rtp_packet.getsequencenumber();

                //this is the highest seq num received

                //print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                        + rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //compute stats and update the label in GUI
                statExpRtpNb++;
                if (seqNb > statHighSeqNb) {
                    statHighSeqNb = seqNb;
                }
                if (statExpRtpNb != seqNb) {
                    statCumLost++;
                }
                statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
                statFractionLost = (float)statCumLost / statHighSeqNb;
                statTotalBytes += payload_length;
                updateStatsLabel();

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                fsynch.addFrame(toolkit.createImage(payload, 0, payload_length), seqNb);

                //display the image as an ImageIcon object
                icon = new ImageIcon(fsynch.nextFrame());
                framePlace.setIcon(icon);
            }
            catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }
    }

    private void updateStatsLabel() {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        statLabel1.setText("Total Bytes Received: " + statTotalBytes);
        statLabel2.setText("Packet Lost Rate: " + formatter.format(statFractionLost));
        statLabel3.setText("Data Rate: " + formatter.format(statDataRate) + " bytes/s");
    }*/
}
