/*
 * Copyright (c) 2024 Moin
 * Java Project For Advanced Programming Practices
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

// keep the following docs together:
// 1. haarcascade_eye.xml
// 2. Video.mp4
package org.example;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

import java.sql.*;


public class DDS extends JFrame {
    private static PreparedStatement insertPatientStmt;

    private static void connectDatabase() {
        String dbUrl = "jdbc:mysql://localhost:3306/dds_db";
        String dbUser = "root";
        String dbPassword = "toor";

        try {
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            String insertPatientSQL = "INSERT INTO details (name, age, gender, symptom, avgdil) VALUES (?, ?, ?, ?, ?)";
            insertPatientStmt = conn.prepareStatement(insertPatientSQL);
            System.out.println("Database connection established and statement prepared.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu adminMenu = new JMenu("Admin");

        JMenuItem loginMenuItem = new JMenuItem("Login");
        loginMenuItem.addActionListener(e -> showLoginDialog());
        adminMenu.add(loginMenuItem);

        menuBar.add(adminMenu);
        setJMenuBar(menuBar);
    }

    // Login dialog
    private void showLoginDialog() {
        JDialog loginDialog = new JDialog(this, "Admin Login", true);
        loginDialog.setSize(300, 200);
        loginDialog.setLayout(new GridLayout(3, 2));

        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField();
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if ("admin".equals(username) && "password123".equals(password)) {
                loginDialog.dispose();
                displayPatientDetails();
            } else {
                JOptionPane.showMessageDialog(loginDialog, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginDialog.add(usernameLabel);
        loginDialog.add(usernameField);
        loginDialog.add(passwordLabel);
        loginDialog.add(passwordField);
        loginDialog.add(new JLabel()); // Spacer
        loginDialog.add(loginButton);

        loginDialog.setLocationRelativeTo(this);
        loginDialog.setVisible(true);
    }

    // Method to display patient details in a new frame
    private void displayPatientDetails() {
        JFrame tableFrame = new JFrame("Patient Details");
        tableFrame.setSize(600, 400);
        tableFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columnNames = {"SNo", "Name", "Age", "Gender", "Symptom", "Avg Dilation"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        tableFrame.add(scrollPane, BorderLayout.CENTER);

        String dbUrl = "jdbc:mysql://localhost:3306/dds_db";
        String dbUser = "root";
        String dbPassword = "toor";

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String query = "SELECT * FROM details";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                tableModel.setRowCount(0);

                while (rs.next()) {
                    int id = rs.getInt("sno");
                    String name = rs.getString("name");
                    int age = rs.getInt("age");
                    String gender = rs.getString("gender");
                    String symptom = rs.getString("symptom");
                    double avgdil = rs.getDouble("avgdil");

                    tableModel.addRow(new Object[]{id, name, age, gender, symptom, avgdil});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data from database.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        tableFrame.setVisible(true);
    }

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final VideoCapture cap;
    private static final List<Double> radius = new ArrayList<>();
    private boolean blink = false;
    private final CascadeClassifier eyeCascade;
    private final JLabel videoPanel;
    private final JFrame videoFrame;

    public DDS() {
        setTitle("Drug Detection System");
        setSize(400, 500);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        createMenuBar();
        getContentPane().setBackground(Color.DARK_GRAY);

        JLabel title = new JLabel("Drug Detection System");
        title.setBounds(50, 20, 400, 40);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.RED);
        add(title);

        JLabel info = new JLabel("<html>Drug abuse can affect several aspects<br>"
                + "of a person's physical and psychological health.<br>"
                + "We can detect this by observing pupil dilation after drug intake.</html>");
        info.setBounds(50, 70, 300, 100);
        info.setForeground(Color.LIGHT_GRAY);
        add(info);

        JButton startButton = new JButton("Start the test for dilation");
        startButton.setBounds(75, 300, 250, 40);
        add(startButton);

        JButton exitButton = new JButton("Exit");
        exitButton.setBounds(150, 350, 100, 40);
        add(exitButton);


        // Initialize video panel for playback
        videoFrame = new JFrame("Dilation Test Video");
        videoFrame.setSize(640, 480);
        videoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        videoFrame.setLayout(new BorderLayout());
        videoPanel = new JLabel();
        videoFrame.add(videoPanel, BorderLayout.CENTER);

        startButton.addActionListener(e -> {
            videoFrame.setVisible(true);
            // start dilation method
            new Thread(this::dilation).start();
        });

        exitButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, "You will be leaving this GUI now.");
            System.exit(0);
        });


        // Load the eye cascade classifier and the video playback
        eyeCascade = new CascadeClassifier("C:/Users/ASUS PC/IdeaProjects/DDS/src/main/java/org/example/haarcascade_eye.xml");
        cap = new VideoCapture("C:/Users/ASUS PC/IdeaProjects/DDS/src/main/java/org/example/Video.mp4");

        if (!cap.isOpened()) {
            System.out.println("Error: Video file could not be opened.");
        }
    }

    // Method for pupil dilation detection
    public void dilation() {
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

        if (!cap.isOpened()) {
            System.out.println("Error: Video file could not be opened.");
            return;
        }

        new SwingWorker<Void, BufferedImage>() {
            @Override
            protected Void doInBackground() throws Exception {
                Mat img = new Mat();
                while (cap.read(img)) {
                    // Process the frame as before
                    Mat gray = new Mat();
                    Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
                    MatOfRect eyes = new MatOfRect();
                    eyeCascade.detectMultiScale(gray, eyes);

                    if (eyes.toArray().length > 0) {
                        if (blink) {
                            blink = false;
                        }
                        Imgproc.putText(img, "Detecting for Dilation...", new org.opencv.core.Point(10, 30), Imgproc.FONT_HERSHEY_COMPLEX_SMALL, 1, new Scalar(0, 255, 0), 2);
                        for (Rect eye : eyes.toArray()) {
                            Imgproc.rectangle(img, eye, new Scalar(0, 255, 0), 2);
                            Mat roiGray = new Mat(gray, eye);
                            Mat blur = new Mat();
                            Imgproc.GaussianBlur(roiGray, blur, new Size(5, 5), 10);
                            Mat erosion = new Mat();
                            Imgproc.erode(blur, erosion, kernel, new org.opencv.core.Point(-1, -1), 2);
                            Mat thresh = new Mat();
                            Imgproc.threshold(erosion, thresh, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

                            Mat circles = new Mat();
                            Imgproc.HoughCircles(roiGray, circles, Imgproc.HOUGH_GRADIENT, 6, 1000, 50, 30, 1, 40);
                            if (!circles.empty()) {
                                for (int i = 0; i < circles.cols(); i++) {
                                    double[] circle = circles.get(0, i);
                                    int r = (int) Math.round(circle[2]);
                                    if (r > 20) {
                                        // Convert pixels to mm
                                        radius.add(r * 0.2645833333);
                                    }
                                }
                            }
                        }
                    } else {
                        if (!blink) {
                            blink = true;
                            Imgproc.putText(img, "Eye not found", new org.opencv.core.Point(10, 90), Imgproc.FONT_HERSHEY_COMPLEX_SMALL, 1, new Scalar(0, 0, 255), 2);
                        }
                    }

                    // call the method to convert for displaying in swing
                    BufferedImage image = matToBufferedImage(img);
                    publish(image);

                    // delay for real-time video update
                    try {
                        Thread.sleep(1); // faster playback since lower num value
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                cap.release();
                // call for plotting graph
                plotGraph();
                return null;
            }

            @Override
            protected void process(List<BufferedImage> chunks) {
                for (BufferedImage img : chunks) {
                    videoPanel.setIcon(new ImageIcon(img));
                    videoPanel.repaint();
                }
            }
        }.execute();
    }

    // Plotting graph
    public void plotGraph() {
        List<Double> filteredRadius = new ArrayList<>();
        for (int i = 0; i < radius.size(); i += 10) {
            filteredRadius.add(radius.get(i));
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < filteredRadius.size(); i++) {
            dataset.addValue(filteredRadius.get(i), "Dilation", Integer.toString(i));
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Pupil Dilation Over Time",
                "Time (Seconds)",
                "Pupil Dilation (mm)",
                dataset
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        JFrame chartFrame = new JFrame();
        chartFrame.setTitle("Pupil Dilation Graph");
        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chartFrame.setContentPane(chartPanel);
        chartFrame.pack();
        chartFrame.setVisible(true);
        JOptionPane.showMessageDialog(null, "Enter Patient Details Now...");
        new PatientDetailsForm();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DDS frame = new DDS();
            frame.setVisible(true);
        });
    }

    // convert mat to buffered image
    // Java image format commonly used for displaying images in Swing
    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] buffer = new byte[bufferSize];
        mat.get(0, 0, buffer);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        return image;
    }
    public static double calculateAverageDilation() {
        if (radius.isEmpty()) return 0.0; // for logic error
        double sum = 0.0;
        for (Double r : radius) {
            sum += r;
        }
        System.out.println(sum/radius.size());
        return sum / radius.size();
    }
    // entering the details of the patient
    public static class PatientDetailsForm extends JFrame {
        public PatientDetailsForm() {
            connectDatabase();
            setTitle("Patient Details");
            setSize(400, 300);
            setLayout(new GridLayout(5, 2));
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.DARK_GRAY);

            JLabel nameLabel = new JLabel("Name:");
            nameLabel.setForeground(Color.LIGHT_GRAY);
            add(nameLabel);
            JTextField nameField = new JTextField();
            add(nameField);

            JLabel ageLabel = new JLabel("Age:");
            ageLabel.setForeground(Color.LIGHT_GRAY);
            add(ageLabel);
            JTextField ageField = new JTextField();
            add(ageField);

            JLabel genderLabel = new JLabel("Gender:");
            genderLabel.setForeground(Color.LIGHT_GRAY);
            add(genderLabel);
            JComboBox<String> genderField = new JComboBox<>(new String[]{"Male", "Female", "Other"});
            add(genderField);

            JLabel symptomsLabel = new JLabel("Symptoms:");
            symptomsLabel.setForeground(Color.LIGHT_GRAY);
            add(symptomsLabel);
            JTextField symptomsField = new JTextField();
            add(symptomsField);

            setSize(450, 350);

            JLabel avgDilationLabel = new JLabel("Average Dilation: " + calculateAverageDilation() + " mm");
            add(avgDilationLabel);


            JButton submitButton = new JButton("Submit");
            add(submitButton);


            submitButton.addActionListener(e -> {
                String name = nameField.getText();
                String age = ageField.getText();
                String gender = (String) genderField.getSelectedItem();
                String symptom = symptomsField.getText();
                String avgDilation = Double.toString(calculateAverageDilation());

                // insert patient details into database
                try {
                    insertPatientStmt.setString(1, name);
                    insertPatientStmt.setString(2, age);
                    insertPatientStmt.setString(3, gender);
                    insertPatientStmt.setString(4, symptom);
                    insertPatientStmt.setString(5, avgDilation);
                    insertPatientStmt.executeUpdate();
                    System.out.println("Patient details inserted successfully.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                dispose();
            });
            setVisible(true);


        }
    }
}
