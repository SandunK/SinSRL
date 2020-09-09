package sinhala.semantic.annotator;

/**
 *
 * Class for a Role of a Frame. Each role is represented by a Token (its syntactic head), and its role label.
 *
 * Created by Alan Akbik on 8/29/17.
 */
public class Role {

    // Label of this role.
    String roleLabel;

    // Syntactic head of this role.
    Token roleHead;

    // constituent of Role
    String constituent;

    /**
     * Constructor for role.
     * @param roleLabel Label of this role
     * @param roleHead Syntactic head of this role
     * @param constituent Role constituent
     */
    public Role(String roleLabel, Token roleHead, String constituent) {
        this.roleLabel = roleLabel;
        this.roleHead = roleHead;
        // ADDED BY ME
        this.constituent = constituent;
    }

    // ------------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------------
    public String getRoleLabel() {
        return roleLabel;
    }

    public Token getRoleHead() {
        return roleHead;
    }

    // ADDED BY ME
    public String getConstituent() {
        return constituent;
    }


}
