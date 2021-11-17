package gitlet;

import java.io.Serializable;

public class Head implements Serializable {

    String HEAD; // sha1 of the current commit
    String branch; // name of the branch

    public Head(String head, String branch) {
        this.HEAD = head;
        this.branch = branch;
    }


}
