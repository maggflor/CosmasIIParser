import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

public class CosmasIIParser extends JFrame implements ActionListener {
    private static final String title = "Cosmas II Parser";
    private DefaultTableModel model = new DefaultTableModel(new String[]{"Result", "Count"}, 0);
    private JTable table = new JTable(){
        @Override
        public void changeSelection(int i, int i1, boolean b, boolean b1) {
            super.changeSelection(i, i1, true, b1);
        }

        @Override
        public void setModel(TableModel tableModel) {
            super.setModel(tableModel);
            if (getRowSorter() instanceof TableRowSorter)
                ((TableRowSorter<TableModel>)getRowSorter()).setModel(tableModel);
        }
    };
    private Vector save;
    private JButton buttonImport = new JButton("Import");
    private JButton buttonZoomIn = new JButton("aA↑");
    private JButton buttonZoomOut = new JButton("Aa↓");
    private JButton buttonDelete = new JButton("Delete");
    private JButton buttonDeselect = new JButton("Deselect");
    private JButton buttonCombine = new JButton("Combine");
    private JButton buttonUndo = new JButton("Undo");
    private JButton buttonOpen = new JButton("Open");
    private JButton buttonSave = new JButton("Save");
    private JFileChooser chooser = new JFileChooser();
    private JLabel labelCount = new JLabel("0");
    private JLabel labelSelected = new JLabel("0");

    public static void main(String[] args) {
        new CosmasIIParser();
    }

    private CosmasIIParser() {
        super(title);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(17, 1));
        panel.add(new JLabel());
        panel.add(buttonImport);
        panel.add(buttonZoomIn);
        panel.add(buttonZoomOut);
        panel.add(new JLabel());
        panel.add(buttonDelete);
        panel.add(buttonDeselect);
        panel.add(buttonCombine);
        panel.add(new JLabel());
        panel.add(buttonUndo);
        panel.add(buttonOpen);
        panel.add(buttonSave);
        panel.add(new JLabel());
        panel.add(new JLabel("Rows:"));
        panel.add(labelCount);
        panel.add(new JLabel("Selected:"));
        panel.add(labelSelected);
        JPanel west = new JPanel();
        west.add(panel);
        add(west, BorderLayout.WEST);

        table.setModel(model);
        table.setDefaultEditor(Object.class, null);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(listSelectionEvent -> labelSelected.setText("" + table.getSelectedRowCount()));
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(){
            @Override
            public void modelStructureChanged() {
                Comparator comparator = getComparator(1);
                super.modelStructureChanged();
                setComparator(1, comparator);
            }
        };
        sorter.setModel(model);
        sorter.setComparator(1, (o1, o2) -> (int)o2 - (int)o1);
        table.setRowSorter(sorter);
        model.addRow(new String[]{"Please import or reopen a file."});
        save = model.getDataVector();
        buttonImport.addActionListener(this);
        buttonZoomIn.addActionListener(this);
        buttonZoomOut.addActionListener(this);
        buttonDelete.addActionListener(this);
        buttonDeselect.addActionListener(this);
        buttonCombine.addActionListener(this);
        buttonUndo.addActionListener(this);
        buttonSave.addActionListener(this);
        buttonOpen.addActionListener(this);

        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == buttonImport)
            importFile();
        else if (actionEvent.getSource() == buttonZoomIn)
            zoomIn();
        else if (actionEvent.getSource() == buttonZoomOut)
            zoomOut();
        else if (actionEvent.getSource() == buttonDelete)
            delete();
        else if (actionEvent.getSource() == buttonDeselect)
            deselect();
        else if (actionEvent.getSource() == buttonUndo)
            undo();
        else if (actionEvent.getSource() == buttonCombine)
            combine();
        else if (actionEvent.getSource() == buttonOpen)
            open();
        else if (actionEvent.getSource() == buttonSave)
            save();
    }

    private void open() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Cosmas II Parser file (.cip)", "cip");
        chooser.setFileFilter(filter);
        chooser.setSelectedFile(null);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            openFile(chooser.getSelectedFile());
        }
    }

    private void openFile(File file) {
        saveState();
        setTitle(title + " - Loading ...");
        setEnabled(false);
        model.setRowCount(0);
        try (ObjectInputStream reader = new ObjectInputStream(new FileInputStream(file))) {
            model.setDataVector((Vector)reader.readObject(), new Vector<>(Arrays.asList(model.getColumnName(0), model.getColumnName(1))));
        } catch (IOException | ClassNotFoundException e) {
            errorMessage(e);
        } finally {
            setTitle(title);
            setEnabled(true);
        }
        labelCount.setText("" + model.getRowCount());
    }

    private void save() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Cosmas II Parser file (.cip)", "cip");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showSaveDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".cip"))
                file = new File(file.getPath() + ".cip");
            saveFile(file);
        }
    }

    private void saveFile(File file) {
        setTitle(title + " - Saving ...");
        setEnabled(false);
        try (ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(file))) {
            writer.writeObject(model.getDataVector());
        } catch (IOException e) {
            errorMessage(e);
        } finally {
            setTitle(title);
            setEnabled(true);
        }
    }

    private void errorMessage(Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        JOptionPane.showMessageDialog(this, sw.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void combine() {
        if (table.getSelectedRow() != -1) {
            String infinitive = JOptionPane.showInputDialog(this, "What is the infinitive form of the combined entries?", "Combination", JOptionPane.QUESTION_MESSAGE);
            if(infinitive != null){
                saveState();
                int count = 0;
                while (table.getSelectedRow() != -1) {
                    int index = table.convertRowIndexToModel(table.getSelectedRow());
                    count += (int) model.getValueAt(index, 1);
                    model.removeRow(index);
                }
                model.addRow(new Object[]{infinitive, count});
                labelCount.setText("" + model.getRowCount());
            }
        }
    }

    private void saveState() {
        save = new Vector<Object>(model.getDataVector());
        buttonUndo.setText("Undo");
    }

    private void undo() {
        Vector<Object> temp = new Vector<Object>(model.getDataVector());
        model.setDataVector(save, new Vector<>(Arrays.asList(model.getColumnName(0), model.getColumnName(1))));
        save = temp;
        if ("Undo".equals(buttonUndo.getText())) {
            buttonUndo.setText("Redo");
        } else if ("Redo".equals(buttonUndo.getText())) {
            buttonUndo.setText("Undo");
        }
        labelCount.setText("" + model.getRowCount());
    }

    private void delete() {
        if (table.getSelectedRow() != -1) {
            saveState();
            while (table.getSelectedRow() != -1) {
                model.removeRow(table.convertRowIndexToModel(table.getSelectionModel().getMaxSelectionIndex()));
            }
            labelCount.setText("" + model.getRowCount());
        }
    }

    private void deselect() {
        table.clearSelection();
    }

    private void importFile() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Cosmas II exported file (.ascii, .rtf)", "ascii", "rtf");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            extractLines(chooser.getSelectedFile());
        }
    }

    private void extractLines(File file) {
        saveState();
        setTitle(title + " - Loading ...");
        setEnabled(false);
        model.setRowCount(0);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("WPD"))
                    model.addRow(new Object[]{line.substring(line.indexOf("\\b") + 3), 1});
            }
            processLines();
            labelCount.setText("" + model.getRowCount());
        } catch (IOException e) {
            errorMessage(e);
        } finally {
            setTitle(title);
            setEnabled(true);
        }
    }

    private void processLines() {
        for (int i = 0; i < model.getRowCount(); ++i) {
            String str = (String)model.getValueAt(i, 0);
            str = str.substring(0, str.indexOf("}"));
            int count = (int)model.getValueAt(i, 1);
            model.removeRow(i);
            for (int j = i; j < model.getRowCount(); ) {
                if (((String)model.getValueAt(j, 0)).startsWith(str)) {
                    count += (int)model.getValueAt(j, 1);
                    model.removeRow(j);
                } else ++j;
            }
            model.insertRow(i, new Object[]{str, count});
        }
    }

    private void zoomIn() {
        Font font = table.getFont();
        font = font.deriveFont(font.getSize() + 1f);
        table.setFont(font);
        table.setRowHeight(font.getSize() + 6);
    }

    private void zoomOut() {
        Font font = table.getFont();
        font = font.deriveFont(font.getSize() - 1f);
        table.setFont(font);
        table.setRowHeight(font.getSize() + 6);
    }
}
