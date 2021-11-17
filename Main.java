package gitlet;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Jackson Qi
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                validateNumArgs(1, args);
                Repository.initCommand();
                break;
            case "add":
                validateNumArgs(2, args);
                Repository.exitIfnoInit();
                Repository.addCommand(args[1]);
                break;
            case "commit":
                if (args.length < 2 || args[1].isEmpty()) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                } else {
                    Repository.exitIfnoInit();
                    Repository.commitCommand(args[1], null);
                }
                break;
            case "rm":
                validateNumArgs(2, args);
                Repository.exitIfnoInit();
                Repository.rmCommand(args[1]);
                break;
            case "log":
                validateNumArgs(1, args);
                Repository.exitIfnoInit();
                Repository.logCommand();
                break;
            case "global-log":
                validateNumArgs(1, args);
                Repository.exitIfnoInit();
                Repository.globalLogCommand();
                break;
            case "find":
                validateNumArgs(2, args);
                Repository.exitIfnoInit();
                Repository.findCommand(args[1]);
                break;
            case "status":
                validateNumArgs(1, args);
                Repository.exitIfnoInit();
                Repository.statusCommand();
                break;
            case "checkout":
                if (args.length == 3) {
                    isValidCheckout1(args);
                    Repository.exitIfnoInit();
                    Repository.checkoutCommand1(args[2]);
                    break;
                } else if (args.length == 4) {
                    isValidCheckout2(args);
                    Repository.exitIfnoInit();
                    Repository.checkoutCommand2(args[1], args[3]);
                    break;
                } else if (args.length == 2) {
                    Repository.exitIfnoInit();
                    Repository.checkoutCommand3(args[1]);
                    break;
                }
                break;
            case "branch":
                validateNumArgs(2, args);
                Repository.exitIfnoInit();
                Repository.branchCommand(args[1]);
                break;
            case "rm-branch":
                validateNumArgs(2, args);
                Repository.rmBranchCommand(args[1]);
                break;
            case "reset":
                validateNumArgs(2, args);
                Repository.resetCommand(args[1]);
                break;
            case "merge":
                validateNumArgs(2, args);
                Repository.mergeCommand(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
        return;
    }

    public static void validateNumArgs(int expected, String[] args) {
        if (expected != args.length) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void isValidCheckout1(String[] args) {
        if (!args[1].equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void isValidCheckout2(String[] args) {
        if (!args[2].equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
