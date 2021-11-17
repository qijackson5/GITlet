package gitlet;

import java.text.SimpleDateFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;


import static gitlet.Utils.serialize;
import static gitlet.Utils.sha1;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Jackson Qi
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    private String id;
    private String timestamp;
    private String message;
    // key = name of file, value = sha1 of contents + fileName
    HashMap<String, String> filesList;
    private String parent;
    private String parent2;
    /** for formatting date
     //  Wed Dec 31 16:00:00 1969 -0800
     // EEE MMM dd HH:mm:ss yyyy Z
     String pattern = "EEE MMM dd HH:mm:ss yyyy Z";
     SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

     String date = simpleDateFormat.format(new Date());
     System.out.println(date);
     */

    // initial commit constructor with 0 arguments
    public Commit() {
        this.id = sha1(serialize(this));
        this.parent = null;
        this.timestamp = getTimestamp(); // come back to this
        this.filesList = new HashMap<>();
        this.message = "initial commit";
    }

    public Commit(String message, String parent) {
        this.id = sha1(serialize(this));
        this.timestamp = getTimestamp();
        this.message = message;
        this.filesList = new HashMap<>();
        this.parent = parent;
        this.parent2 = null;
    }

    @Override
    public String toString() {
        String output = "";
        output += "===\n";
        output += ("commit " + sha1(serialize(this)) + "\n");
        if (parent2 != null) {
            output += "Merge: " + parent.substring(0, 7) + " "
                    + parent2.substring(0, 7) + "\n";
        }
        output += ("Date: " + timestamp + "\n");
        output += (message + "\n");
//        for (String name : filesList.keySet()) {
//            output += name + ", ";
//        }
        return output;
    }

    public String getId() {
        return sha1(serialize(this));
    }

    public String getTimestamp() {
        String datePattern = "EEE MMM dd HH:mm:ss yyyy Z";
        Date theDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
        String formattedTimestamp = sdf.format(theDate);
        return formattedTimestamp;
    }

    public String getMessage() {
        return message;
    }

    public HashMap<String, String> getFilesList() {
        return filesList;
    }

    public String getParent() {
        return parent;
    }

    public void setParent2(String parent2) {
        this.parent2 = parent2;
    }

}
