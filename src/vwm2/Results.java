package vwm2;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class Results extends JFrame{
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel text;

    public Results() {
        createGUI();

    }
    public void fill(List<Map.Entry<String, Double>> data){
        data.forEach((row) -> {
            tableModel.addRow(new Object[]{row.getKey(),row.getValue()});
        });
    }
    public void setText(String newText){
        text.setText(newText);
    }

    private void createGUI() {
        setLayout(new BorderLayout());
        JScrollPane pane = new JScrollPane();
        table = new JTable();
        pane.setViewportView(table);
        JPanel eastPanel = new JPanel();
        JPanel northPanel = new JPanel();
        text = new JLabel("asd");
        add(northPanel, BorderLayout.NORTH);
        add(eastPanel, BorderLayout.EAST);
        add(pane,BorderLayout.CENTER);
        add(text, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Dokument","CosSim"},0);
        table.setModel(tableModel);
    }

} 