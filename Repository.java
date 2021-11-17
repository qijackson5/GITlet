package gitlet;

import java.io.File;
import static gitlet.Utils.*;
import java.io.IOException;
import java.util.*;

/** Represents a gitlet repository.
 *  This class contains all the command methods we need to get the version control working
 *  @author Jackson Qi
 */

public class Repository {
    private static Head HEAD;
    private static HashMap<String, String> branches;
    private static HashSet<String> blob;

    // stagingAreaAdd and Remove are objects that should be stored in the stagingArea directory
    private static HashMap<String, String> stagingAreaAdd = new HashMap<>();
    private static HashMap<String, String> stagingAreaRemove = new HashMap<>();

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits_dir");
    public static final File STAGING_AREA = join(GITLET_DIR, "stagingArea");
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs_dir");

    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    public static final File BRANCHES_FILE = join(GITLET_DIR, "branches");

    public static void initCommand() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            COMMITS_DIR.mkdir();
            STAGING_AREA.mkdir();
            BLOBS_DIR.mkdir();
            branches = new HashMap<>();
            Utils.writeObject(join(STAGING_AREA, "stagingAreaAdd"), stagingAreaAdd);
            Utils.writeObject(join(STAGING_AREA, "stagingAreaRemove"), stagingAreaRemove);
            writeObject(join(BLOBS_DIR, "blobStorage"), new HashMap<String, Blob>());

            Commit initialCommit = new Commit();
            File initial = Utils.join(COMMITS_DIR, sha1(serialize(initialCommit)));
            Utils.writeObject(initial, initialCommit);

            branches.put("master", sha1(serialize(initialCommit)));
            HEAD = new Head(branches.get("master"), "master");
            Utils.writeObject(BRANCHES_FILE, branches);
            Utils.writeObject(HEAD_FILE, HEAD);

        } else {
            System.out.println("A Gitlet version-control system "
                            + "already exists in the current directory.");
            System.exit(0);
        }
    }


    public static void addCommand(String fileName) {
//        File file = Utils.join(CWD, "testing/src/" + fileName);
        File file = Utils.join(CWD, fileName);
        checkFileExists(file); // would exit if the file does not exist, otherwise exists
        // if exists, I add it to the staging area add
        String[] contents = new String[]{fileName, Utils.readContentsAsString(file)};

        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        HashMap<String, String> stagingRm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        stagingAdd.put(fileName, sha1(contents));

//        System.out.println("StaginAdd After Add" + stagingAdd.toString());

        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        Commit currentCommit =
                readObject(join(COMMITS_DIR, HEAD.HEAD), Commit.class); // most recent commit
        // if the current commit has an identical file, then remove it from the staging area
        if (isIdentical(currentCommit, fileName, file)) {
            stagingAdd.remove(fileName);
        }
        if (stagingRm.containsKey(fileName)) {
            stagingRm.remove(fileName);
        }
        Utils.writeObject(join(STAGING_AREA, "stagingAreaAdd"), stagingAdd);
        Utils.writeObject(join(STAGING_AREA, "stagingAreaRemove"), stagingRm);

//        System.out.println(stagingAdd.toString());
    }

    public static void commitCommand(String message, String parent2) {
        if (message.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        Commit newCommit = new Commit(message, branches.get(HEAD.branch));

        if (parent2 != null) {
            newCommit.setParent2(parent2);
        }

        // add every files in the staging area to a the file list in this commit object
        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        if (stagingAdd.isEmpty() && stagingrm.isEmpty() && parent2 == null) {
            System.out.println("No changes added to the commit.");
            return;
        }
        HashMap<String, Blob> blobStorage =
                readObject(join(BLOBS_DIR, "blobStorage"), HashMap.class);
        Commit currCommit = grabCommit(HEAD.HEAD);

        // grab all files in the currCommit (in staging add but not in staging remove
        for (String fileName : currCommit.filesList.keySet()) {
            if (!stagingAdd.containsKey(fileName) && !stagingrm.containsKey(fileName)) {
                newCommit.filesList.put(fileName, currCommit.getFilesList().get(fileName));
            }
        }

        // add all the ones that have been modified in staging add
        for (String fileName: stagingAdd.keySet()) {
            // key = name of file, value = sha1 of contents + fileName
            newCommit.filesList.put(fileName, stagingAdd.get(fileName));
            if (!blobStorage.containsKey(stagingAdd.get(fileName))) {
                File file = Utils.join(CWD, fileName);
//                File file = Utils.join(CWD, "testing/src/" + fileName);
                String contents = readContentsAsString(file);
                Blob newBlob = new Blob(fileName, stagingAdd.get(fileName), contents);
                blobStorage.put(stagingAdd.get(fileName), newBlob);
            }
        }


//        System.out.println("current blob Map: " + blobStorage.toString());
//        System.out.println("new commit: " + newCommit.toString());

        String sha1ofNewCommit = sha1(serialize(newCommit));
        File thisCommit = join(COMMITS_DIR, sha1ofNewCommit);
        Utils.writeObject(thisCommit, newCommit);

        // clear the staging area
        stagingAdd.clear();
        stagingrm.clear();
        Utils.writeObject(join(STAGING_AREA, "stagingAreaAdd"), stagingAdd);
        Utils.writeObject(join(STAGING_AREA, "stagingAreaRemove"), stagingrm);

        branches.put(HEAD.branch, sha1ofNewCommit);
        HEAD.HEAD = sha1ofNewCommit;

        Utils.writeObject(join(BLOBS_DIR, "blobStorage"), blobStorage);
        Utils.writeObject(join(GITLET_DIR, "branches"), branches);
        Utils.writeObject(join(GITLET_DIR, "HEAD"), HEAD);
    }

    public static void rmCommand(String fileName) {
//        File file = Utils.join(CWD, "testing/src/" + fileName);
        File file = join(CWD, fileName);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);
        Commit currentCommit = readObject(join(COMMITS_DIR, HEAD.HEAD), Commit.class);

        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);


        if (currentCommit.getFilesList().containsKey(fileName)) {
            stagingrm.put(fileName, currentCommit.getFilesList().get(fileName));
            restrictedDelete(file);
            writeObject(join(STAGING_AREA, "stagingAreaRemove"), stagingrm);
        } else if (stagingAdd.containsKey(fileName)) {
            stagingAdd.remove(fileName);
            writeObject(join(STAGING_AREA, "stagingAreaAdd"), stagingAdd);
        } else {
            System.out.println("No reason to remove the file.");
        }
        return;
    }

    public static void logCommand() {
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        Commit currCommit = grabCommit(HEAD.HEAD);

        while (currCommit != null) {
            System.out.println(currCommit);
            if (currCommit.getParent() == null) {
                break;
            }
            currCommit = grabCommit(currCommit.getParent());
        }
    }

    public static void globalLogCommand() {
        List<String> commitsInSha1 = plainFilenamesIn(COMMITS_DIR);

        for (String sha1 : commitsInSha1) {
            Commit thisCommit = readObject(join(COMMITS_DIR, sha1), Commit.class);
            System.out.println(thisCommit);
        }
    }

    public static void findCommand(String message) {
        List<String> commitsInSha1 = plainFilenamesIn(COMMITS_DIR);
        boolean commitsExist = false;
        for (String sha1 : commitsInSha1) {
            Commit thisCommit = readObject(join(COMMITS_DIR, sha1), Commit.class);
            if (thisCommit.getMessage().equals(message)) {
                commitsExist = true;
                System.out.println(thisCommit.getId());
            }
        }
        if (!commitsExist) {
            System.out.println("Found no commit with that message.");
            return;
        }
    }

    public static void statusCommand() {
        System.out.println("=== Branches ===");
        printBranches();
        System.out.println("=== Staged Files ===");
        printStageAdd();
        System.out.println("=== Removed Files ===");
        printStageRemove();
        System.out.println("=== Modifications Not Staged For Commit ===\n");
        System.out.println("=== Untracked Files ===");
//        printUntrackedFilesEC();
        return;
    }

    public static void checkoutCommand1(String fileName) {
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        Commit currCommit = readObject(join(COMMITS_DIR, HEAD.HEAD), Commit.class);
        HashMap<String, String> filesList = currCommit.getFilesList();
        HashMap<String, Blob> blobStorage =
                readObject(join(BLOBS_DIR, "blobStorage"), HashMap.class);
        // Commit.files list: key = name of file, value = sha1 of contents + fileName
        if (!filesList.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String sha1ofFile = filesList.get(fileName);
        Blob fileBlob = blobStorage.get(sha1ofFile);

        String fileContent = fileBlob.getContents();

        File file = join(CWD, fileName);
        restoreFile(file, fileContent);
    }

    public static void checkoutCommand2(String commitID, String fileName) {
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        Commit foundCommit = findCommitByIDinCurrentBranch(commitID);
        if (foundCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        HashMap<String, String> filesList = foundCommit.getFilesList();

        HashMap<String, Blob> blobStorage =
                readObject(join(BLOBS_DIR, "blobStorage"), HashMap.class);
        // Commit.fileslist: key = name of file, value = sha1 of contents + fileName
        if (!filesList.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        String sha1ofFile = filesList.get(fileName);
        Blob fileBlob = blobStorage.get(sha1ofFile);

        String fileContent = fileBlob.getContents();

        File file = join(CWD, fileName);
        restoreFile(file, fileContent);

        return;
    }

    public static void checkoutCommand3(String branchName) {
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        } else if (HEAD.branch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        HashMap<String, String> currFileList =
                readObject(join(COMMITS_DIR, HEAD.HEAD), Commit.class).getFilesList();
        HashMap<String, String> destFileList =
                readObject(join(COMMITS_DIR, branches.get(branchName)),
                        Commit.class).getFilesList();

        HashMap<String, Blob> blobStorage =
                readObject(join(BLOBS_DIR, "blobStorage"), HashMap.class);

        Set<Blob> currBlobs = new HashSet<>();
        Set<Blob> destBlobs = new HashSet<>();

        for (String hash : currFileList.values()) {
            currBlobs.add(blobStorage.get(hash));
        }
        for (String hash : destFileList.values()) {
            destBlobs.add(blobStorage.get(hash));
        }

        Set<String> untracked = getUntrackedFiles();
        for (String fileName : untracked) {
            if (destFileList.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        restoreFiles(currBlobs, destBlobs);

        stagingAdd.clear();
        stagingrm.clear();

        //move the head to the branch
        HEAD.HEAD = branches.get(branchName);
        HEAD.branch = branchName;

        writeObject(join(STAGING_AREA, "stagingAreaAdd"), stagingAdd);
        writeObject(join(STAGING_AREA, "stagingAreaRemove"), stagingrm);

        Utils.writeObject(join(GITLET_DIR, "branches"), branches);
        Utils.writeObject(join(GITLET_DIR, "HEAD"), HEAD);
    }


    public static void branchCommand(String branchName) {
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        branches.put(branchName, HEAD.HEAD);
        //HEAD.branch = branchName;

        Utils.writeObject(join(GITLET_DIR, "branches"), branches);
        //Utils.writeObject(join(GITLET_DIR, "HEAD"), HEAD);
    }

    public static void rmBranchCommand(String branchName) {
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        String currentBranch = HEAD.branch;
        if (currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        } else if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        branches.remove(branchName);
        writeObject(join(GITLET_DIR, "branches"), branches);
        return;
    }

    public static void resetCommand(String commitID) {
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        HashMap<String, Blob> blobStorage =
                readObject(join(BLOBS_DIR, "blobStorage"), HashMap.class);

        Set<Blob> currBlobs = new HashSet<>();
        Set<Blob> destBlobs = new HashSet<>();

        Commit currentCommit = grabCommit(HEAD.HEAD);
        Commit destinationCommit = grabCommit(commitID);

        if (destinationCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }

        for (String fileHash : currentCommit.filesList.values()) {
            currBlobs.add(blobStorage.get(fileHash));
        }
        for (String fileHash : destinationCommit.filesList.values()) {
            destBlobs.add(blobStorage.get(fileHash));
        }



        Set<String> untracked = getUntrackedFiles();

        for (String fileName : untracked) {
            if (destinationCommit.getFilesList().containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        restoreFiles(currBlobs, destBlobs);

        HEAD.HEAD = commitID;
        branches.put(HEAD.branch, commitID);

        // HAVE TO CLEAR STAGING AREA
        stagingAdd.clear();
        stagingrm.clear();

        writeObject(join(STAGING_AREA, "stagingAreaAdd"), stagingAdd);
        writeObject(join(STAGING_AREA, "stagingAreaRemove"), stagingrm);
        writeObject(join(GITLET_DIR, "HEAD"), HEAD);
        writeObject(join(GITLET_DIR, "branches"), branches);
    }

    public static void mergeCommand(String branchName) {
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);

        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        Commit currentCommit = grabCommit(HEAD.HEAD);
        Commit incomingCommit =
                readObject(join(COMMITS_DIR, branches.get(branchName)), Commit.class);

        Commit splitCommit = findIntersectingCommit(currentCommit, incomingCommit);
        if (stagingAreaIsNotEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        } else if (HEAD.branch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        } else if (checkUntrackedwillgetOverriden(incomingCommit)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        } else if (incomingCommit.getId().equals(splitCommit.getId())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (currentCommit.getId().equals(splitCommit.getId())) {
            checkoutCommand3(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        // step 1
        mergeStep1(currentCommit, splitCommit, incomingCommit);

        // step 2 (check if the files in current branch has been modified since the split point
        // but stayed the same in the given branch

        // step 3
        // step 4
        // step 5
        mergeStep5(currentCommit, splitCommit, incomingCommit);

        // step 6
        mergeStep6(currentCommit, splitCommit, incomingCommit);

        // step 7
        //System.out.println(intersectingCommit.toString());

        // step 8
        Map<String, Blob[]> conflicts = getMergeConflicts(currentCommit,
                splitCommit, incomingCommit);

        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        if (conflicts.isEmpty()) {
            writeFilesToCWD(stagingAdd, stagingrm);
        } else {
            writeConflicts(conflicts);
            System.out.println("Encountered a merge conflict.");
        }
        String message = "Merged " + branchName + " into " + HEAD.branch + ".";
        commitCommand(message, incomingCommit.getId());
    }

    public static boolean checkUntrackedwillgetOverriden(Commit incoming) {
        Set<String> untracked = getUntrackedFiles();
        for (String fileName : untracked) {
            if (incoming.getFilesList().containsKey(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static void writeFilesToCWD(HashMap<String, String> stagingAdd,
                                       HashMap<String, String> stagingrm) {
        for (String fileName1 : stagingAdd.keySet()) {
            File file1 = join(CWD, fileName1);
            writeContents(file1, grabBlob(stagingAdd.get(fileName1)).getContents());
        }

        for (String fileName2 : stagingrm.keySet()) {
            File file2 = join(CWD, fileName2);
            restrictedDelete(file2);
        }
    }

    public static void writeConflicts(Map<String, Blob[]> conflicts) {
        for (String fileName : conflicts.keySet()) {
            File cwdFile = join(CWD, fileName);

            if (conflicts.get(fileName)[0] != null && conflicts.get(fileName)[1] != null) {
                String curr = conflicts.get(fileName)[0].getContents();
                String inc = conflicts.get(fileName)[1].getContents();
                String output = "<<<<<<< HEAD\n" + curr
                        + "\n" + "=======\n" + inc + "\n" + ">>>>>>>";
                writeContents(cwdFile, output);

            } else if (conflicts.get(fileName)[0] == null) { // curr is empty
                String contents = conflicts.get(fileName)[1].getContents();
                String output = "<<<<<<< HEAD\n"  + "=======\n" + contents + ">>>>>>>\n";
                Utils.writeContents(cwdFile, output);

            } else if (conflicts.get(fileName)[1] == null) { // incoming is empty
                String contents = conflicts.get(fileName)[0].getContents();
                String output = "<<<<<<< HEAD\n" + contents + "=======\n" + ">>>>>>>\n";
                Utils.writeContents(cwdFile, output);
            }
            addCommand(fileName);
        }
    }

    // file same in curr and split, but different in incoming
    public static void mergeStep1(Commit curr, Commit split, Commit incoming) {
        HashMap<String, String> currFiles = curr.getFilesList();
        HashMap<String, String> splitFiles = split.getFilesList();
        HashMap<String, String> incomingFiles = incoming.getFilesList();

        List<String> unmodifiedCurrentFiles = new ArrayList<>();
        for (String fileName : currFiles.keySet()) {
            if (splitFiles.containsKey(fileName)) {
                if (currFiles.get(fileName).equals(splitFiles.get(fileName))) {
                    unmodifiedCurrentFiles.add(fileName);
                }
            }
        }

        List<String> modifiedIncomingFiles = new ArrayList<>();
        for (String fileName : unmodifiedCurrentFiles) {
            if (incomingFiles.containsKey(fileName)) {
                if (!splitFiles.get(fileName).equals(incomingFiles.get(fileName))) {
                    modifiedIncomingFiles.add(fileName);
                }
            }
        }
        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        Set<Blob> modifiedBranchBlobs = new HashSet<>();
        for (String fileName: modifiedIncomingFiles) {
            Blob fileBlob = grabBlob(incomingFiles.get(fileName));
            restoreFile(join(CWD, fileName), fileBlob.getContents());
            stagingAdd.put(fileName, incomingFiles.get(fileName));
        }
        writeObject(join(STAGING_AREA, "stagingAreaAdd"), stagingAdd);
    }

    // file exist in incoming but not in curr and split
    public static void mergeStep5(Commit curr, Commit split, Commit incoming) {
        HashMap<String, String> currFiles = curr.getFilesList();
        HashMap<String, String> splitFiles = split.getFilesList();
        HashMap<String, String> incomingFiles = incoming.getFilesList();

        HashMap<String, Blob> blobStorage =
                readObject(join(BLOBS_DIR, "blobStorage"), HashMap.class);

        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        for (String fileName : incomingFiles.keySet()) {
            if (!splitFiles.containsKey(fileName) && !currFiles.containsKey(fileName)) {
                stagingAdd.put(fileName, incomingFiles.get(fileName));
                File file = join(CWD, fileName);
                writeContents(file, blobStorage.get(incomingFiles.get(fileName)).getContents());
            }
        }
        writeObject(join(STAGING_AREA, "stagingAreaAdd"), stagingAdd);
    }

    // file are same in curr and split, but does NOT EXIST in incoming
    public static void mergeStep6(Commit curr, Commit split, Commit incoming) {
        HashMap<String, String> currFiles = curr.getFilesList();
        HashMap<String, String> splitFiles = split.getFilesList();
        HashMap<String, String> incomingFiles = incoming.getFilesList();

        List<String> unmodifiedCurrentFiles = new ArrayList<>();
        for (String fileName : splitFiles.keySet()) {
            if (currFiles.containsKey(fileName)) {
                if (currFiles.get(fileName).equals(splitFiles.get(fileName))) {
                    unmodifiedCurrentFiles.add(fileName);
                }
            }
        }

        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        for (String fileName : unmodifiedCurrentFiles) {
            if (!incomingFiles.containsKey(fileName)) {
                restrictedDelete(fileName);
                stagingrm.put(fileName, currFiles.get(fileName));
            }
        }
        writeObject(join(STAGING_AREA, "stagingAreaRemove"), stagingrm);
    }

    // conflicts with 3 cases
    public static Map<String, Blob[]> getMergeConflicts(Commit curr,
                                                        Commit split, Commit incoming) {
        HashMap<String, String> currFiles = curr.getFilesList();
        HashMap<String, String> splitFiles = split.getFilesList();
        HashMap<String, String> incomingFiles = incoming.getFilesList();

        // file name, current, incoming
        Map<String, Blob[]> conflicts = new HashMap<>();

        // files are all different
        for (String fileName : splitFiles.keySet()) {
            if (currFiles.containsKey(fileName) && incomingFiles.containsKey(fileName)) {
                if (!currFiles.get(fileName).equals(incomingFiles.get(fileName))
                        && !splitFiles.get(fileName).equals(currFiles.get(fileName))
                        && !splitFiles.get(fileName).equals(incomingFiles.get(fileName))) {
                    conflicts.put(fileName, new Blob[]{grabBlob(currFiles.get(fileName)),
                            grabBlob(incomingFiles.get(fileName))});
                }
            }
        }

        // part b. file modified in one branch w/ respect split, deleted in the other
        for (String fileName : splitFiles.keySet()) {
            if ((currFiles.containsKey(fileName)
                    && !currFiles.get(fileName).equals(splitFiles.get(fileName))
                    && !incomingFiles.containsKey(fileName))
                    ||
                    (incomingFiles.containsKey(fileName)
                    && !incomingFiles.get(fileName).equals(splitFiles.get(fileName))
                    && !currFiles.containsKey(fileName))
            ) {

                conflicts.put(fileName, new Blob[]{grabBlob(currFiles.get(fileName)),
                        grabBlob(incomingFiles.get(fileName))});
            }
        }

        // part c: does not exist in split but modified in both
        for (String fileName : currFiles.keySet()) {
            if (!splitFiles.containsKey(fileName) && incomingFiles.containsKey(fileName)
                && !currFiles.get(fileName).equals(incomingFiles.get(fileName))) {
                conflicts.put(fileName, new Blob[]{grabBlob(currFiles.get(fileName)),
                        grabBlob(incomingFiles.get(fileName))});
            }
        }

        return conflicts;
    }

    // grab blob given file name
    public static Blob grabBlob(String fileName) {
        HashMap<String, Blob> blobStorage =
                readObject(join(BLOBS_DIR, "blobStorage"), HashMap.class);
        if (fileName == null) {
            return null;
        }
        return blobStorage.get(fileName);
    }



    // restore just 1 file in the CWD
    public static void restoreFile(File file, String fileContent) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeContents(file, fileContent);
    }

    // restores the files in the CWD
    public static void restoreFiles(Set<Blob> currBlobs, Set<Blob> destBlobs) {

        List<Blob> blobsToBeDeleted = new ArrayList<>();
        for (Blob blob1 : currBlobs) {

            if (!destBlobs.contains(blob1)) {
                //remove that file
                File file = Utils.join(CWD, blob1.fileName);
//                File file = Utils.join(CWD, "testing/src/" + blob.fileName);
                if (file.exists()) {
                    file.delete();
                    blobsToBeDeleted.add(blob1);
                }
            }
        }

        for (Blob b : blobsToBeDeleted) {
            currBlobs.remove(b);
        }

        for (Blob blob2 : destBlobs) {
//            File file = Utils.join(CWD, "testing/src/" + blob.fileName);
            File file = join(CWD, blob2.fileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
//            System.out.println(blob.getContents());
            writeContents(file, blob2.getContents());
        }

    }

    public static void exitIfnoInit() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }


    // this is for the addCommand to check if the file exists in the CWD, if doesn't then will exit
    public static void checkFileExists(File file) {
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
    }

    // for addCommand to check if the file is identical to any files in the most recent commit
    public static boolean isIdentical(Commit currCommit, String fileName, File file) {

        String[] contents = new String[]{fileName, Utils.readContentsAsString(file)};

        return currCommit.filesList != null
                && currCommit.getFilesList().containsKey(fileName)
                && (currCommit.getFilesList().get(fileName).equals(sha1(contents)));
    }

    // returns the commit given the sha1hash
    public static Commit grabCommit(String sha1hash) {
        if (sha1hash == null) {
            return null;
        }
        List<String> listOfCommits = plainFilenamesIn(COMMITS_DIR);
        for (String commitID : listOfCommits) {
            if (commitID.equals(sha1hash) || commitID.contains(sha1hash)) {
                File file = join(COMMITS_DIR, sha1hash);
                return readObject(file, Commit.class);
            }
        }
        return null;
    }

    // prints all the current branches
    public static void printBranches() {
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);
        branches = readObject(join(GITLET_DIR, "branches"), HashMap.class);
        List<String> listOfBranches = new ArrayList<>();
        listOfBranches.addAll(branches.keySet());
        printSortedBranches(listOfBranches);
    }
    // sort and print branches
    public static void printSortedBranches(List<String> branchesList) {
        String currentBranch = HEAD.branch;
        Collections.sort(branchesList);
        for (String s : branchesList) {
            if (s.equals(currentBranch)) {
                System.out.println("*" + currentBranch);
            } else {
                System.out.println(s);
            }
        }
        System.out.println();
    }


    // prints the name of all the files staged for add
    public static void printStageAdd() {
        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        List<String> addList = new ArrayList<>();
        addList.addAll(stagingAdd.keySet());
        printSorted(addList);
    }

    // prints the names of all the files in the staged for remove
    public static void printStageRemove() {
        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        List<String> rmList = new ArrayList<>();
        rmList.addAll(stagingrm.keySet());
        printSorted(rmList);
    }


    // prints out the list of fileNames alphabetically
    public static void printSorted(List<String> list) {
        Collections.sort(list);
        for (String s : list) {
            System.out.println(s);
        }
        System.out.println();
    }

    public static Commit findCommitByIDinCurrentBranch(String sha1) {
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);
        Commit currCommit = readObject(join(COMMITS_DIR, HEAD.HEAD), Commit.class);

        while (currCommit != null) {
            if (currCommit.getId().equals(sha1)
                    || currCommit.getId().contains(sha1)) {
                return currCommit;
            }
            currCommit = currCommit.getParent() == null
                    ? null :  grabCommit(currCommit.getParent());
        }

        return null;
    }

    //get all untracked files in CWD

    public static Set<String> getUntrackedFiles() {
        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);

        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);
        Commit currCommit = readObject(join(COMMITS_DIR, HEAD.HEAD), Commit.class);

        Set<String> untracked = new HashSet<>();

        for (String fileName : plainFilenamesIn(CWD)) {
            if ((!currCommit.getFilesList().containsKey(fileName))
                    && (!stagingAdd.containsKey(fileName))) {
                untracked.add(fileName);
            }
        }
        return untracked;
    }

    public static Commit findIntersectingCommit(Commit commit1, Commit commit2) {
        Set<String> firstSet = new HashSet<>();

        Commit c1 = commit1;
        Commit c2 = commit2;

        while (c1 != null) {
            firstSet.add(c1.getId());
            c1 = grabCommit(c1.getParent());
        }

        //System.out.println("First Set: " + firstSet);

        while (c2 != null) {
            if (firstSet.contains(c2.getId())) {
                return c2;
            }
            c2 = grabCommit(c2.getParent());
        }

        return null;
    }

//    public static void printModificationsNotStagedForCommit() {
//        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);
//        Commit currCommit = readObject(join(COMMITS_DIR, HEAD.HEAD), Commit.class);
//
//        List<String> result = new ArrayList<>();
//        for (String fileName : plainFilenamesIn(CWD)) {
//            if (currCommit.getFilesList().containsKey(fileName))
//        }
//    }

    public static void printUntrackedFilesEC() {
        HEAD = readObject(join(GITLET_DIR, "HEAD"), Head.class);
        Commit currCommit = readObject(join(COMMITS_DIR, HEAD.HEAD), Commit.class);

        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        List<String> untrackedFiles = new ArrayList<>();

        for (String fileName : stagingrm.keySet()) {
            File file = join(CWD, fileName);
            if (file.exists()) { // meaning it was recreated after removal
                untrackedFiles.add(fileName);
            }
        }
        for (String fileName : plainFilenamesIn(CWD)) {
            if (!stagingAdd.containsKey(fileName)
                    && !currCommit.getFilesList().containsKey(fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        printSorted(untrackedFiles);
    }

    public static boolean stagingAreaIsNotEmpty() {
        HashMap<String, String> stagingAdd =
                readObject(join(STAGING_AREA, "stagingAreaAdd"), HashMap.class);
        HashMap<String, String> stagingrm =
                readObject(join(STAGING_AREA, "stagingAreaRemove"), HashMap.class);

        return (!stagingAdd.isEmpty() || !stagingrm.isEmpty());
    }
}
