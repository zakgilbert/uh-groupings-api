package edu.hawaii.its.api.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.hawaii.its.api.type.Grouping;
import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.hawaii.its.api.type.SyncDestination;

import edu.internet2.middleware.grouperClient.ws.beans.WsAssignAttributesResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsAssignGrouperPrivilegesLiteResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsAttributeAssign;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetAttributeAssignmentsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubjectLookup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("groupAttributeService")
public class GroupAttributeServiceImpl implements GroupAttributeService {

    @Value("${groupings.api.settings}")
    private String SETTINGS;

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

    @Value("${groupings.api.releasedgrouping}")
    private String RELEASED_GROUPING;

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

    public static final Log logger = LogFactory.getLog(GroupAttributeServiceImpl.class);

    @Autowired
    private GrouperFactoryService grouperFactoryService;

    @Autowired
    private HelperService helperService;

    @Autowired
    private MemberAttributeService memberAttributeService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private GroupingAssignmentService groupingAssignmentService;

    /**
     * Get all the sync destinations for a specific grouping.
     */
    @Override
    public List<SyncDestination> getAllSyncDestinations(String currentUsername, String path) {

        if (!memberAttributeService.isAdmin(currentUsername)) {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }

        Grouping grouping = groupingAssignmentService.getGrouping(path, currentUsername);
        List<SyncDestination> finSyncDestList = getAllSyncDestinations();

        for (SyncDestination dest : finSyncDestList) {
            dest.setDescription(dest.parseKeyVal(grouping.getName(), dest.getDescription()));
        }

        return finSyncDestList;
    }

    @Override
    public List<SyncDestination> getAllSyncDestinations() {
        return grouperFactoryService.getSyncDestinations();
    }

    /**
     * Similar to the getAllSyncDestination except it is called through getGrouping and thus doesn't check to see if
     * person requesting the information is an owner or superuser as that has already been checked.
     */
    @Override
    public List<SyncDestination> getSyncDestinations(Grouping grouping) {
        List<SyncDestination> syncDestinations = getAllSyncDestinations();

        if (syncDestinations == null) {
            return null;
        }
        for (SyncDestination destination : syncDestinations) {
            destination.setIsSynced(isGroupAttribute(grouping.getPath(), destination.getName()));
            destination.setDescription(destination.parseKeyVal(grouping.getName(), destination.getDescription()));
        }
        return syncDestinations;
    }

    /**
     * Turn the ability for users to opt-in to a grouping on or off.
     */
    @Override
    public List<GroupingsServiceResult> changeOptInStatus(String groupingPath, String ownerUsername,
            boolean isOptInOn) {
        List<GroupingsServiceResult> results = new ArrayList<>();

        if (!memberAttributeService.isOwner(ownerUsername) && !memberAttributeService.isAdmin(ownerUsername)) {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }

        results.add(assignGrouperPrivilege(EVERY_ENTITY, PRIVILEGE_OPT_IN, groupingPath + INCLUDE, isOptInOn));
        results.add(assignGrouperPrivilege(EVERY_ENTITY, PRIVILEGE_OPT_OUT, groupingPath + EXCLUDE, isOptInOn));
        results.add(changeGroupAttributeStatus(groupingPath, ownerUsername, OPT_IN, isOptInOn));

        return results;
    }

    /**
     * Turn the ability for users to opt-out of a grouping on or off.
     */
    @Override
    public List<GroupingsServiceResult> changeOptOutStatus(String groupingPath, String ownerUsername,
            boolean isOptOutOn) {

        List<GroupingsServiceResult> results = new ArrayList<>();

        if (!memberAttributeService.isOwner(ownerUsername) && !memberAttributeService.isAdmin(ownerUsername)) {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }

        results.add(assignGrouperPrivilege(EVERY_ENTITY, PRIVILEGE_OPT_IN, groupingPath + EXCLUDE, isOptOutOn));
        results.add(assignGrouperPrivilege(EVERY_ENTITY, PRIVILEGE_OPT_OUT, groupingPath + INCLUDE, isOptOutOn));
        results.add(changeGroupAttributeStatus(groupingPath, ownerUsername, OPT_OUT, isOptOutOn));

        return results;
    }

    // Turns the attribute on or off in a group.
    // OPT_IN, OPT_OUT, and sync destinations are allowed.
    @Override
    public GroupingsServiceResult changeGroupAttributeStatus(String groupPath, String ownerUsername,
            String attributeName, boolean turnAttributeOn) {
        GroupingsServiceResult gsr;

        String verb = "removed from ";
        if (turnAttributeOn) {
            verb = "added to ";
        }

        String action = attributeName + " has been " + verb + groupPath + " by " + ownerUsername;

        if (!memberAttributeService.isOwner(ownerUsername) && !memberAttributeService.isAdmin(ownerUsername)) {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }

        boolean isHasAttribute = isGroupAttribute(groupPath, attributeName);

        if (turnAttributeOn) {
            if (!isHasAttribute) {
                assignGroupAttributes(attributeName, OPERATION_ASSIGN_ATTRIBUTE, groupPath);

                gsr = helperService.makeGroupingsServiceResult(SUCCESS, action);

                membershipService.updateLastModified(groupPath);
            } else {
                gsr = helperService
                        .makeGroupingsServiceResult(SUCCESS + ", " + attributeName + " already existed", action);
            }
        } else {
            if (isHasAttribute) {
                assignGroupAttributes(attributeName, OPERATION_REMOVE_ATTRIBUTE, groupPath);

                gsr = helperService.makeGroupingsServiceResult(SUCCESS, action);

                membershipService.updateLastModified(groupPath);
            } else {
                gsr = helperService
                        .makeGroupingsServiceResult(SUCCESS + ", " + attributeName + " did not exist", action);
            }
        }

        return gsr;
    }

    // Returns true if the group has the attribute with that name.
    public boolean isGroupAttribute(String groupPath, String attributeName) {
        WsGetAttributeAssignmentsResults wsGetAttributeAssignmentsResults = attributeAssignmentsResults(
                ASSIGN_TYPE_GROUP,
                groupPath,
                attributeName);

        if (wsGetAttributeAssignmentsResults.getWsAttributeAssigns() != null) {
            for (WsAttributeAssign attribute : wsGetAttributeAssignmentsResults.getWsAttributeAssigns()) {
                if (attribute.getAttributeDefNameName() != null && attribute.getAttributeDefNameName()
                        .equals(attributeName)) {
                    return true;
                }
            }
        }
        return false;
    }


    // Checks to see if a group has an attribute of a specific type and returns the list if it does.
    @Override
    public WsGetAttributeAssignmentsResults attributeAssignmentsResults(String assignType, String groupPath,
            String attributeName) {
        logger.info("attributeAssignmentsResults; assignType: "
                + assignType
                + "; group: "
                + groupPath
                + "; nameName: "
                + attributeName
                + ";");

        return grouperFactoryService.makeWsGetAttributeAssignmentsResultsForGroup(assignType, attributeName, groupPath);
    }

    // Adds, removes, updates (operationName) the attribute for the group.
    public GroupingsServiceResult assignGroupAttributes(String attributeName, String attributeOperation,
            String groupPath) {
        logger.info("assignGroupAttributes; "
                + "; attributeName: "
                + attributeName
                + "; attributeOperation: "
                + attributeOperation
                + "; group: "
                + groupPath
                + ";");

        WsAssignAttributesResults attributesResults = grouperFactoryService.makeWsAssignAttributesResultsForGroup(
                ASSIGN_TYPE_GROUP,
                attributeOperation,
                attributeName,
                groupPath);

        return helperService.makeGroupingsServiceResult(attributesResults,
                "assign " + attributeName + " attribute to " + groupPath);
    }

    //gives the user the privilege for that group
    public GroupingsServiceResult assignGrouperPrivilege(
            String username,
            String privilegeName,
            String groupPath,
            boolean isSet) {

        logger.info("assignGrouperPrivilege; username: "
                + username
                + "; group: "
                + groupPath
                + "; privilegeName: "
                + privilegeName
                + " set: "
                + isSet
                + ";");

        WsSubjectLookup lookup = grouperFactoryService.makeWsSubjectLookup(username);
        String action = "set " + privilegeName + " " + isSet + " for " + username + " in " + groupPath;

        WsAssignGrouperPrivilegesLiteResult grouperPrivilegesLiteResult =
                grouperFactoryService.makeWsAssignGrouperPrivilegesLiteResult(
                        groupPath,
                        privilegeName,
                        lookup,
                        isSet);

        return helperService.makeGroupingsServiceResult(grouperPrivilegesLiteResult, action);
    }

    // Updates a Group's description, then passes the Group object to GrouperFactoryService to be saved in Grouper.
    public GroupingsServiceResult updateDescription(String groupPath, String ownerUsername, String description) {
        logger.info("updateDescription(); groupPath:" + groupPath +
                "; ownerUsername:" + ownerUsername +
                "; description: " + description + ";");

        GroupingsServiceResult gsr;

        String action = "Description field of grouping " + groupPath + " has been updated by " + ownerUsername;

        if (!memberAttributeService.isOwner(groupPath, ownerUsername) && !memberAttributeService
                .isAdmin(ownerUsername)) {

            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }

        grouperFactoryService.updateGroupDescription(groupPath, description);

        gsr = helperService.makeGroupingsServiceResult(SUCCESS + ", description updated", action);

        return gsr;
    }

}
