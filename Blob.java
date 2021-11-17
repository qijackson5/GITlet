package gitlet;

import java.io.Serializable;


public class Blob implements Serializable {

    String fileName;
    String fileSha1;
    String contents;

    public Blob(String fileName, String fileSha1, String contents) {
        this.fileName = fileName;
        this.fileSha1 = fileSha1;
        this.contents = contents;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSha1() {
        return fileSha1;
    }

    public String getContents() {
        return contents;
    }

}
