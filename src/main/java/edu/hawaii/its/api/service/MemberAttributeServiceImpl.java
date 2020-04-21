package edu.hawaii.its.api.service;

import edu.hawaii.its.api.repository.PersonRepository;
import edu.hawaii.its.api.type.GenericServiceResult;
import edu.hawaii.its.api.type.Group;
import edu.hawaii.its.api.type.Grouping;
import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.hawaii.its.api.type.Person;

import edu.internet2.middleware.grouperClient.ws.GcWebServiceError;
import edu.internet2.middleware.grouperClient.ws.beans.WsAddMemberResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsAttributeAssign;
import edu.internet2.middleware.grouperClient.ws.beans.WsDeleteMemberResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetAttributeAssignmentsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetMembershipsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetSubjectsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsHasMemberResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsHasMemberResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubject;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubjectLookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("memberAttributeService")
public class MemberAttributeServiceImpl implements MemberAttributeService {

    @Value("${groupings.api.settings}")
    private String SETTINGS;

    @Value("${groupings.api.test.admin_user}")
    private String ADMIN;

    @Value("${groupings.api.grouping_admins}")
    private String GROUPING_ADMINS;

    @Value("${groupings.api.grouping_apps}")
    private String GROUPING_APPS;

    @Value("${groupings.api.grouping_owners}")
    private String GROUPING_OWNERS;

    @Value("${groupings.api.grouping_superusers}")
    private String GROUPING_SUPERUSERS;

    @Value("${groupings.api.attributes}")
    private String ATTRIBUTES;

    @Value("${groupings.api.for_groups}")
    private String FOR_GROUPS;

    @Value("${groupings.api.for_memberships}")
    private String FOR_MEMBERSHIPS;

    @Value("${groupings.api.last_modified}")
    private String LAST_MODIFIED;

    @Value("${groupings.api.yyyymmddThhmm}")
    private String YYYYMMDDTHHMM;

    @Value("${groupings.api.uhgrouping}")
    private String UHGROUPING;

    @Value("${groupings.api.destinations}")
    private String DESTINATIONS;

    @Value("${groupings.api.listserv}")
    private String LISTSERV;

    @Value("${groupings.api.trio}")
    private String TRIO;

    @Value("${groupings.api.purge_grouping}")
    private String PURGE_GROUPING;

    @Value("${groupings.api.self_opted}")
    private String SELF_OPTED;

    @Value("${groupings.api.anyone_can}")
    private String ANYONE_CAN;

    @Value("${groupings.api.opt_in}")
    private String OPT_IN;

    @Value("${groupings.api.opt_out}")
    private String OPT_OUT;

    @Value("${groupings.api.basis}")
    private String BASIS;

    @Value("${groupings.api.basis_plus_include}")
    private String BASIS_PLUS_INCLUDE;

    @Value("${groupings.api.exclude}")
    private String EXCLUDE;

    @Value("${groupings.api.include}")
    private String INCLUDE;

    @Value("${groupings.api.owners}")
    private String OWNERS;

    @Value("${groupings.api.grouping_owners}")
    private String OWNERS_GROUP;

    @Value("${groupings.api.assign_type_group}")
    private String ASSIGN_TYPE_GROUP;

    @Value("${groupings.api.assign_type_immediate_membership}")
    private String ASSIGN_TYPE_IMMEDIATE_MEMBERSHIP;

    @Value("${groupings.api.subject_attribute_name_uhuuid}")
    private String SUBJECT_ATTRIBUTE_NAME_UID;

    @Value("${groupings.api.operation_assign_attribute}")
    private String OPERATION_ASSIGN_ATTRIBUTE;

    @Value("${groupings.api.operation_remove_attribute}")
    private String OPERATION_REMOVE_ATTRIBUTE;

    @Value("${groupings.api.operation_replace_values}")
    private String OPERATION_REPLACE_VALUES;

    @Value("${groupings.api.privilege_opt_out}")
    private String PRIVILEGE_OPT_OUT;

    @Value("${groupings.api.privilege_opt_in}")
    private String PRIVILEGE_OPT_IN;

    @Value("${groupings.api.every_entity}")
    private String EVERY_ENTITY;

    @Value("${groupings.api.is_member}")
    private String IS_MEMBER;

    @Value("${groupings.api.success}")
    private String SUCCESS;

    @Value("${groupings.api.failure}")
    private String FAILURE;

    @Value("${groupings.api.success_allowed}")
    private String SUCCESS_ALLOWED;

    @Value("${groupings.api.stem}")
    private String STEM;

    @Value("${groupings.api.person_attributes.uhuuid}")
    private String UHUUID;

    @Value("${groupings.api.person_attributes.username}")
    private String UID;

    @Value("${groupings.api.person_attributes.first_name}")
    private String FIRST_NAME;

    @Value("${groupings.api.person_attributes.last_name}")
    private String LAST_NAME;

    @Value("${groupings.api.person_attributes.composite_name}")
    private String COMPOSITE_NAME;

    @Value("${groupings.api.insufficient_privileges}")
    private String INSUFFICIENT_PRIVILEGES;

    @Autowired
    private GrouperFactoryService grouperFS;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private HelperService hs;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private GroupingAssignmentService groupingAssignmentService;

    public static final Log logger = LogFactory.getLog(MemberAttributeServiceImpl.class);

    @Override
    public boolean isOwner(String username) {
        return isMember(OWNERS_GROUP, username);
    }

    //return true if the membership between the group and user has the self-opted attribute, false otherwise
    @Override
    public boolean isSelfOpted(String groupPath, String username) {
        logger.info("isSelfOpted; group: " + groupPath + "; username: " + username + ";");

        if (isMember(groupPath, username)) {
            WsGetMembershipsResults wsGetMembershipsResults = hs.membershipsResults(username, groupPath);
            String membershipID = hs.extractFirstMembershipID(wsGetMembershipsResults);

            WsAttributeAssign[] wsAttributes =
                    getMembershipAttributes(ASSIGN_TYPE_IMMEDIATE_MEMBERSHIP, SELF_OPTED, membershipID);

            for (WsAttributeAssign att : wsAttributes) {
                if (att.getAttributeDefNameName() != null) {
                    if (att.getAttributeDefNameName().equals(SELF_OPTED)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    //give ownership to a new user
    @Override
    public GroupingsServiceResult assignOwnership(String groupingPath, String ownerUsername, String newOwnerUsername) {
        logger.info("assignOwnership; groupingPath: "
                + groupingPath
                + "; ownerUsername: "
                + ownerUsername
                + "; newOwnerUsername: "
                + newOwnerUsername
                + ";");
        String action;
        GroupingsServiceResult ownershipResult;

        if (isUhUuid(newOwnerUsername)) {
            action = "give user with id " + newOwnerUsername + " ownership of " + groupingPath;
        } else {
            action = "give " + newOwnerUsername + " ownership of " + groupingPath;
        }

        if (isOwner(groupingPath, ownerUsername) || isSuperuser(ownerUsername)) {
            WsSubjectLookup user = grouperFS.makeWsSubjectLookup(ownerUsername);
            WsAddMemberResults amr = grouperFS.makeWsAddMemberResults(groupingPath + OWNERS, user, newOwnerUsername);

            ownershipResult = hs.makeGroupingsServiceResult(amr, action);

            membershipService.updateLastModified(groupingPath);
            membershipService.updateLastModified(groupingPath + OWNERS);
        } else {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }

        return ownershipResult;
    }

    //remove ownership of a grouping from a current owner
    @Override
    public GroupingsServiceResult removeOwnership(String groupingPath, String actor, String ownerToRemove) {
        logger.info("removeOwnership; grouping: "
                + groupingPath
                + "; username: "
                + actor
                + "; ownerToRemove: "
                + ownerToRemove
                + ";");

        GroupingsServiceResult ownershipResults;
        String action = "remove ownership of " + groupingPath + " from " + ownerToRemove;

        if (isOwner(groupingPath, actor) || isSuperuser(actor)) {
            WsSubjectLookup lookup = grouperFS.makeWsSubjectLookup(actor);
            WsDeleteMemberResults memberResults = grouperFS.makeWsDeleteMemberResults(
                    groupingPath + OWNERS,
                    lookup,
                    ownerToRemove);
            ownershipResults = hs.makeGroupingsServiceResult(memberResults, action);

            membershipService.updateLastModified(groupingPath);
            membershipService.updateLastModified(groupingPath + OWNERS);

        } else {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }

        return ownershipResults;
    }

    //returns true if the user is a member of the group via username or UH id
    @Override
    public boolean isMember(String groupPath, String username) {
        logger.info("isMember; groupPath: " + groupPath + "; username: " + username + ";");

        if (isUhUuid(username)) {
            return isMemberUuid(groupPath, username);
        } else {
            WsHasMemberResults memberResults = grouperFS.makeWsHasMemberResults(groupPath, username);

            WsHasMemberResult[] memberResultArray = memberResults.getResults();

            for (WsHasMemberResult hasMember : memberResultArray) {
                if (hasMember.getResultMetadata().getResultCode().equals(IS_MEMBER)) {
                    return true;
                }
            }
            return false;
        }
    }

    //returns true if the person is a member of the group
    @Override
    public boolean isMember(String groupPath, Person person) {
        if (person.getUsername() != null) {
            return isMember(groupPath, person.getUsername());
        }

        WsHasMemberResults memberResults = grouperFS.makeWsHasMemberResults(groupPath, person);

        WsHasMemberResult[] memberResultArray = memberResults.getResults();

        for (WsHasMemberResult hasMember : memberResultArray) {
            if (hasMember.getResultMetadata().getResultCode().equals(IS_MEMBER)) {
                return true;
            }
        }
        return false;

    }

    // returns true if the person is a member of the group
    public boolean isMemberUuid(String groupPath, String idnum) {
        logger.info("isMember; groupPath: " + groupPath + "; uuid: " + idnum + ";");

        WsHasMemberResults memberResults = grouperFS.makeWsHasMemberResults(groupPath, idnum);

        WsHasMemberResult[] memberResultArray = memberResults.getResults();

        for (WsHasMemberResult hasMember : memberResultArray) {
            if (hasMember.getResultMetadata().getResultCode().equals(IS_MEMBER)) {
                return true;
            }
        }
        return false;
    }

    //returns true if the user is in the owner group of the grouping
    @Override
    public boolean isOwner(String groupingPath, String username) {
        return isMember(groupingPath + OWNERS, username);
    }

    //returns true if the user is in the admins group
    @Override
    public boolean isAdmin(String username) {
        return isMember(GROUPING_ADMINS, username);
    }

    //returns true if the user is in the apps group
    @Override
    public boolean isApp(String username) {
        return isMember(GROUPING_APPS, username);
    }

    //returns true if the user is in the superusers group
    @Override
    public boolean isSuperuser(String username) {
        return isAdmin(username) || isApp(username);
    }

    // returns true if username is a UH id number
    @Override
    public boolean isUhUuid(String naming) {
        return naming.matches("\\d+");
    }

    //checks to see if a membership has an attribute of a specific type and returns the list if it does
    public WsAttributeAssign[] getMembershipAttributes(String assignType, String attributeUuid, String membershipID) {
        logger.info("getMembershipAttributes; assignType: "
                + assignType
                + "; name: "
                + attributeUuid
                + "; membershipID: "
                + membershipID
                + ";");

        WsGetAttributeAssignmentsResults attributeAssignmentsResults =
                grouperFS.makeWsGetAttributeAssignmentsResultsForMembership(
                        assignType,
                        attributeUuid,
                        membershipID);

        WsAttributeAssign[] wsAttributes = attributeAssignmentsResults.getWsAttributeAssigns();

        return wsAttributes != null ? wsAttributes : grouperFS.makeEmptyWsAttributeAssignArray();
    }

    /*
     * Covered by Integration Tests
     *
     * Returns a user's attributes (FirstName(givenName), LastName(sn), Composite Name(cn), Username(uid), UH User ID(uhUuid)) based on the username.
     * If the requester of the information is not a superuser or owner, then the function returns a mapping with empty values.
     *
     * Not testable with Unit test as needs to connect to Grouper database to work, not mock db.
     *
     */
    public Map<String, String> getUserAttributes(String ownerUsername, String username) throws GcWebServiceError {
        WsSubjectLookup lookup;
        WsGetSubjectsResults results;
        String[] attributeValues = new String[5];
        Map<String, String> mapping = new HashMap<>();

        // Checks to make sure the user requesting information of another is a superuser or the owner of the group.
        if (isSuperuser(ownerUsername) || groupingAssignmentService.groupingsOwned(
                groupingAssignmentService.getGroupPaths(ownerUsername, ownerUsername)).size() != 0) {
            try {

                // Makes a call to GrouperClient and creates a WebService Subject Lookup of specified user.
                lookup = grouperFS.makeWsSubjectLookup(username);

                /*
                 * Using the WebService Subject Lookup it gets the gets the WebService Subject Results.
                 * The results returns information about the user including the user's attributes.
                 * In the results are the attributes and attribute names.
                 */
                results = grouperFS.makeWsGetSubjectsResults(lookup);

                // Maps the attribute to the attribute name.
                for (int i = 0; i < attributeValues.length; i++) {
                    mapping.put(results.getSubjectAttributeNames()[i],
                            results.getWsSubjects()[0].getAttributeValues()[i]);
                }
                return mapping;

            } catch (NullPointerException npe) {
                throw new GcWebServiceError("Error 404 Not Found");
            }
        } else {
            String[] subjectAttributeNames = { UID, COMPOSITE_NAME, LAST_NAME, FIRST_NAME, UHUUID };
            for (int i = 0; i < attributeValues.length; i++) {
                mapping.put(subjectAttributeNames[i], "");
            }
            return mapping;
        }

    }

    // Returns a specific user's attribute (FirstName, LastName, etc.) based on the username
    // Not testable with Unit test as needs to connect to Grouper database to work, not mock db
    public String getSpecificUserAttribute(String ownerUsername, String username, int attribute)
            throws GcWebServiceError {
        WsSubject[] subjects;
        WsSubjectLookup lookup;
        String[] attributeValues = new String[5];
        Map<String, String> mapping = new HashMap<>();

        if (isSuperuser(ownerUsername) || groupingAssignmentService.groupingsOwned(
                groupingAssignmentService.getGroupPaths(ownerUsername, ownerUsername)).size() != 0) {
            try {
                lookup = grouperFS.makeWsSubjectLookup(username);
                WsGetSubjectsResults results = grouperFS.makeWsGetSubjectsResults(lookup);
                subjects = results.getWsSubjects();

                attributeValues = subjects[0].getAttributeValues();
                return attributeValues[attribute];

            } catch (NullPointerException npe) {
                throw new GcWebServiceError("Error 404 Not Found");
            }
        } else {
            return attributeValues[attribute];
        }

    }

    @Override
    public List<Person> searchMembers(String groupPath, String username) {

        List<Person> members = new ArrayList<>();

        WsHasMemberResults results = grouperFS.makeWsHasMemberResults(groupPath, username);
        WsHasMemberResult[] memberResultArray = results.getResults();

        for (WsHasMemberResult hasMember : memberResultArray) {

            if (hasMember.getResultMetadata().getResultCode().equals(IS_MEMBER)) {
                String memberName = hasMember.getWsSubject().getName();
                String memberUuid = hasMember.getWsSubject().getId();
                String memberUsername = hasMember.getWsSubject().getIdentifierLookup();

                members.add(new Person(memberName, memberUuid, memberUsername));
            }
        }
        return members;
    }

    /**
     * Get a GenericServiceResult{groupingsServiceResult: GroupingsServiceResult, isOwner: bool},
     * which tells whether usernameInQuestion is an owner or not.
     *
     * @param currentUser        - current owner.
     * @param usernameInQuestion - user to be authenticated.
     * @return - GenericServiceResult {groupingsServiceResult: GroupingsServiceResult, isOwner: bool }.
     */
    @Override
    public GenericServiceResult getIsOwner(String currentUser, String usernameInQuestion) {
        String action = "getIsOwner: " + "currentUser: " + currentUser + ";, " +
                "usernameInQuestion: " + usernameInQuestion + ";";
        logger.info(action);

        if (isSuperuser(currentUser) || isAdmin(currentUser)) {
            try {
                return new GenericServiceResult(Arrays.asList("groupingsServiceResult", "isOwner"),
                        new GroupingsServiceResult(SUCCESS, action),
                        (groupingAssignmentService.groupingsOwned(groupingAssignmentService.getGroupPaths(
                                currentUser, usernameInQuestion)).size() > 0));
            } catch (GcWebServiceError e) {
                logger.error(action, e);
                return new GenericServiceResult(Collections.singletonList("groupingsServiceResult"),
                        new GroupingsServiceResult(FAILURE, action + ";, " + e.getMessage()));
            }
        }
        throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
    }

    @Override
    public GenericServiceResult getIsAdmin(String currentUser, String usernameInQuestion) {
        String action = "getIsAdmin: " + "currentUser: " + currentUser + ";, " +
                "usernameInQuestion: " + usernameInQuestion + ";";
        logger.info(action);
        if (isSuperuser(currentUser) || isAdmin(currentUser)) {
            try {
                return new GenericServiceResult(Arrays.asList("groupingsServiceResult", "isAdmin"),
                        new GroupingsServiceResult(SUCCESS, action),
                        (groupingAssignmentService.adminLists(
                                usernameInQuestion).getAdminGroup().getMembers().size() > 0));
            } catch (AccessDeniedException | GcWebServiceError e) {
                logger.error(action, e);
                return new GenericServiceResult(Collections.singletonList("groupingsServiceResult"),
                        new GroupingsServiceResult(FAILURE, action + ";, " + e.getMessage()));
            }
        }
        throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
    }

    @Override
    public GenericServiceResult checkAddMember(String groupingPath, String groupPath, String currentUser,
            String userToCheck) {

        if (!isSuperuser(currentUser) && !isAdmin(currentUser)) {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }

        if (!checkUsernameValidity(userToCheck)) {
            return new GenericServiceResult("result",
                    new GroupingsServiceResult(FAILURE, "The uid " + userToCheck + " is invalid"));
        }

        String otherPath = "null";
        String action;
        Person personToAdd;
        boolean isMemberPath;
        boolean isMemberOtherPath = false;
        boolean inBasis = groupingAssignmentService.getGrouping(groupingPath, currentUser).getBasis().getUsernames()
                .contains(userToCheck);

        switch (groupPath) {
            case "include":
                otherPath = groupingPath + ":exclude";
                break;
            case "exclude":
                otherPath = groupingPath + ":include";
                break;
        }
        groupingPath += ":" + groupPath;

        action = "checkAddMembers[ addPath: " + groupingPath + "; deletePath: " + otherPath + "; userToCheck: "
                + userToCheck + ";]";
        try {
            isMemberPath = isMember(groupingPath, userToCheck);

            if (!(isMemberOtherPath = (isMember(otherPath, userToCheck)))) {
                otherPath = "null";
            }
            personToAdd = membershipService.createNewPerson(userToCheck);
            personToAdd.setAttributes(getUserAttributes(currentUser, userToCheck));

            logger.info(action);

            return new GenericServiceResult(
                    Arrays.asList("result", "add", "addPath", "delete", "deletePath", "inBasis"),
                    new GroupingsServiceResult(SUCCESS, action, personToAdd), !isMemberPath,
                    groupingPath, isMemberOtherPath, otherPath, inBasis);
        } catch (GcWebServiceError e) {
            // Path is invalid.
            logger.error(action, e);
            throw new GcWebServiceError(e);
        }
    }

    public boolean checkUsernameValidity(String userToCheck) {
        WsSubjectLookup wsSubjectLookup = grouperFS.makeWsSubjectLookup(userToCheck);
        WsGetSubjectsResults wsGetSubjectsResults = grouperFS.makeWsGetSubjectsResults(wsSubjectLookup);
        return !("SUBJECT_NOT_FOUND".equals(wsGetSubjectsResults.getWsSubjects()[0].getResultCode()));
    }
}
