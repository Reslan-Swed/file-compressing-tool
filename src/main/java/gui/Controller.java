package gui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXSpinner;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;
import utils.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    private JFXRadioButton compressTypeRle;

    @FXML
    private ToggleGroup compressTypeToggles;

    @FXML
    private JFXRadioButton compressTypeHuffman;

    @FXML
    private JFXRadioButton compressTypeAHuffman;

    @FXML
    private JFXRadioButton compressTypeLz77;

    @FXML
    private JFXRadioButton compressTypeLzw;

    @FXML
    private JFXButton compressButton;

    @FXML
    private HBox compressResultBar;

    @FXML
    private JFXSpinner compressRatioProgress;

    @FXML
    private JFXButton decompressButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label compressRatioPercentage;

    @FXML
    private JFXProgressBar statusProgressBar;

    @FXML
    private TextField compressSelectionField;

    @FXML
    private TextField compressSavingField;

    @FXML
    private TextField decompressSelectionField;

    @FXML
    private TextField decompressExtractionField;

    @FXML
    private Hyperlink statusLink;

    private enum CompressType {
        RLE("rle"), HUFFMAN("huffman"), DHUFFMAN("dhuffman"), LZ77("lz77"), LZW("lzw");

        private String value;

        CompressType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private BooleanProperty showCompressStatus;
    private StringProperty statusMessage;
    private BooleanProperty disableButtons;
    private BooleanProperty showStatusProgress;
    private DoubleProperty compressRatio;
    private CompressType compressType;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        compressTypeToggles.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == compressTypeRle) {
                compressType = CompressType.RLE;
            } else if (newValue == compressTypeHuffman) {
                compressType = CompressType.HUFFMAN;
            } else if (newValue == compressTypeAHuffman) {
                compressType = CompressType.DHUFFMAN;
            } else if (newValue == compressTypeLz77) {
                compressType = CompressType.LZ77;
            } else if (newValue == compressTypeLzw) {
                compressType = CompressType.LZW;
            }
        });
        compressType = CompressType.RLE;
        showCompressStatus = new SimpleBooleanProperty(false);
        compressResultBar.visibleProperty().bind(showCompressStatus);
        statusMessage = new SimpleStringProperty("Ready");
        statusLabel.textProperty().bind(statusMessage);
        disableButtons = new SimpleBooleanProperty(false);
        compressButton.disableProperty().bind(disableButtons);
        decompressButton.disableProperty().bind(disableButtons);
        compressRatio = new SimpleDoubleProperty(0d);
        compressRatioProgress.progressProperty().bind(compressRatio);
        showStatusProgress = new SimpleBooleanProperty(false);
        statusProgressBar.visibleProperty().bind(showStatusProgress);

    }

    @FXML
    void onCompressOpenFileHandler(ActionEvent event) {
        File file = openFileChooser(false);
        compressSelectionField.setText(file == null ? "" : file.getAbsolutePath());
    }

    @FXML
    void onCompressOpenFolderHandler(ActionEvent event) {
        File file = openDirectoryChooser();
        compressSelectionField.setText(file == null ? "" : file.getAbsolutePath());
    }

    @FXML
    void onCompressSaveAsHandler(ActionEvent event) {
        File file = openFileChooser(true);
        compressSavingField.setText(file == null ? "" : file.getAbsolutePath());
    }

    @FXML
    void onCompressStartHandler(ActionEvent event) {
        String fileIn = compressSelectionField.getText();
        if (fileIn.isEmpty()) {
            statusMessage.setValue("❗ Please Select a File or Folder to Compress");
            return;
        }
        String fileOut = compressSavingField.getText();
        new CompressTask().execute(fileIn, fileOut);
    }

    @FXML
    void onDecompressExtractToHandler(ActionEvent event) {
        File file = openDirectoryChooser();
        decompressExtractionField.setText(file == null ? "" : file.getAbsolutePath());
    }

    @FXML
    void onDecompressOpenFileHandler(ActionEvent event) {
        File file = openFileChooser(false, new FileChooser.ExtensionFilter("Compressed File", "*.rle", "*.huffman", "*.dhuffman", "*.lz77", "*.lzw"));
        decompressSelectionField.setText(file == null ? "" : file.getAbsolutePath());
    }

    @FXML
    void onDecompressStartHandler(ActionEvent event) {
        String fileIn = decompressSelectionField.getText();
        if (fileIn.isEmpty()) {
            statusMessage.setValue("❗ Please Select a Compressed File to Decompress");
            return;
        }
        String extractionPath = decompressExtractionField.getText();
        new DecompressTask().execute(fileIn, extractionPath);
    }

    private File openFileChooser(boolean asSaveDialog, FileChooser.ExtensionFilter... extensionFilter) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(asSaveDialog ? "Save As" : "Open");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().addAll(extensionFilter);
        return asSaveDialog ? chooser.showSaveDialog(null) : chooser.showOpenDialog(null);
    }

    private File openDirectoryChooser() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Extraction Path");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        return chooser.showDialog(null);
    }

    private class CompressTask extends AsyncTask<String, Void, Long> {

        private Compressor compressor;
        private long originalFileSize;
        private String fileOut;

        @Override
        public void onPreExecute() {
            switch (compressType) {
                case RLE:
                    compressor = new RLE();
                    break;
                case HUFFMAN:
                    compressor = new Huffman();
                    break;
                case DHUFFMAN:
                    compressor = new AdaptiveHuffman();
                    break;
                case LZW:
                    compressor = new LZW();
                    break;
                case LZ77:
                    compressor = new LZ77();
                    break;
            }
            showStatusProgress.setValue(true);
            statusMessage.setValue("🗜 Compressing... Please Wait");
            statusLink.setText("");
            statusLink.setOnAction(Event::consume);
            disableButtons.setValue(true);
            showCompressStatus.setValue(false);
        }

        @Override
        public Long doInBackground(String... params) {
            if (new File(params[0]).isDirectory()) {
                try {
                    Files.list(Paths.get(params[0]))
                            .map(Path::toFile).filter(File::isFile).forEach(f -> originalFileSize += f.length());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                originalFileSize = new File(params[0]).length();
            }
            fileOut = (params[1].isEmpty() ? FilenameUtils.removeExtension(params[0]) : params[1]) + "." + compressType.toString();
            return CompressFactory.compress(params[0], fileOut, compressor);
        }

        @Override
        public void onPostExecute(Long params) {
            showStatusProgress.setValue(false);
            disableButtons.setValue(false);
            if (params != -1) {
                statusMessage.setValue("✔ Compression Done");
                long ratio = 0;
                if (originalFileSize != 0) {
                    ratio = 100 - params * 100 / originalFileSize;
                }
                compressRatio.setValue(ratio / 100d <= 0 ? 0 : ratio / 100d);
                compressRatioPercentage.setText(ratio + "%");
                showCompressStatus.setValue(true);
                String fileDirectory = FilenameUtils.getFullPathNoEndSeparator(new File(fileOut).getAbsolutePath());
                statusLink.setText(fileDirectory + "\\ ");
                statusLink.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().open(new File(fileDirectory));
                    } catch (IOException ex) {
                        System.out.println("Can not open file location " + fileDirectory);
                    }
                });
            } else {
                statusMessage.setValue("❌ Compression Failed !");
            }
        }

        @Override
        public void progressCallback(Void... params) {

        }
    }

    private class DecompressTask extends AsyncTask<String, Void, Boolean> {

        private String extractPath;

        @Override
        public void onPreExecute() {
            showStatusProgress.setValue(true);
            statusMessage.setValue("📤 Decompressing... Please Wait");
            statusLink.setText("");
            statusLink.setOnAction(Event::consume);
            disableButtons.setValue(true);
        }

        @Override
        public Boolean doInBackground(String... params) {
            Compressor compressor = null;
            String ext = FilenameUtils.getExtension(params[0]);
            switch (CompressType.valueOf(ext.toUpperCase())) {
                case RLE:
                    compressor = new RLE();
                    break;
                case HUFFMAN:
                    compressor = new Huffman();
                    break;
                case DHUFFMAN:
                    compressor = new AdaptiveHuffman();
                    break;
                case LZW:
                    compressor = new LZW();
                    break;
                case LZ77:
                    compressor = new LZ77();
                    break;
            }
            extractPath = params[1].isEmpty() ? FilenameUtils.getFullPathNoEndSeparator(new File(params[0]).getAbsolutePath()) : params[1];
            return CompressFactory.decompress(params[0], extractPath, compressor);
        }

        @Override
        public void onPostExecute(Boolean params) {
            showStatusProgress.setValue(false);
            disableButtons.setValue(false);
            if (params) {
                statusMessage.setValue("✔ Extraction Done");
                showCompressStatus.setValue(true);
                statusLink.setText(extractPath);
                statusLink.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().open(new File(extractPath));
                    } catch (IOException ex) {
                        System.out.println("Can not open file location " + extractPath);
                    }
                });
            } else {
                statusMessage.setValue("❌ Extraction Failed !");
            }
        }

        @Override
        public void progressCallback(Void... params) {

        }
    }


}
