package edu.hawaii.its.api.service;

import edu.hawaii.its.api.type.AdminListsHolder;
import edu.hawaii.its.api.type.Group;
import edu.hawaii.its.api.type.Grouping;
import edu.hawaii.its.api.type.GroupingAssignment;
import edu.hawaii.its.api.type.MembershipAssignment;
import edu.hawaii.its.api.type.Person;
import edu.hawaii.its.api.type.SyncDestination;

import edu.internet2.middleware.grouperClient.ws.StemScope;
import edu.internet2.middleware.grouperClient.ws.beans.WsAttributeAssign;
import edu.internet2.middleware.grouperClient.ws.beans.WsAttributeDefName;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetAttributeAssignmentsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetGroupsResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetGroupsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetMembersResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetMembersResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGroup;
import edu.internet2.middleware.grouperClient.ws.beans.WsStemLookup;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubject;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubjectLookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service("groupingAssignmentService")
public class GroupingAssignmentServiceImpl implements GroupingAssignmentService {

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

    @Value("${groupings.api.timeout}")
    private Integer TIMEOUT;

    @Value("${groupings.api.stale_subject_id}")
    private String STALE_SUBJECT_ID;

    @Value("${groupings.api.insufficient_privileges}")
    private String INSUFFICIENT_PRIVILEGES;

    public static final Log logger = LogFactory.getLog(GroupingAssignmentServiceImpl.class);

    @Autowired
    private GrouperFactoryService grouperFactoryService;

    @Autowired
    private HelperService helperService;

    @Autowired
    private MemberAttributeService memberAttributeService;

    @Autowired GroupAttributeService groupAttributeService;

    // returns a list of all of the groups in groupPaths that are also groupings
    @Override
    public List<Grouping> groupingsIn(List<String> groupPaths) {
        List<String> groupingsIn = helperService.extractGroupings(groupPaths);
        List<Grouping> groupings = helperService.makeGroupings(groupingsIn);

        groupings.forEach(this::setGroupingAttributes);

        return groupings;
    }

    @Override
    public List<Grouping> restGroupingsOwned(String actingUsername, String ownerUsername) {
        return groupingsOwned(getGroupPaths(actingUsername, ownerUsername));
    }

    @Override
    public List<Grouping> restGroupingsExclude(String actingUsername, String ownerUsername) {
        return excludeGroups(getGroupPaths(actingUsername, ownerUsername));
    }

    //returns a list of groupings that corresponds to all of the owner groups in groupPaths
    @Override
    public List<Grouping> groupingsOwned(List<String> groupPaths) {
        List<String> ownerGroups = groupPaths
                .stream()
                .filter(groupPath -> groupPath.endsWith(OWNERS))
                .map(groupPath -> groupPath.substring(0, groupPath.length() - OWNERS.length()))
                .collect(Collectors.toList());

        // make sure the owner group actually correspond to a grouping
        List<String> ownedGroupings = helperService.extractGroupings(ownerGroups);

        return helperService.makeGroupings(ownedGroupings);
    }

    @Override
    public List<Grouping> excludeGroups(List<String> groupPaths) {
        List<String> excludeGroups = groupPaths
                .stream()
                .filter(groupPath -> groupPath.endsWith(EXCLUDE))
                .map(groupPath -> groupPath.substring(0, groupPath.length() - EXCLUDE.length()))
                .collect(Collectors.toList());

        // make sure the owner group actually correspond to a grouping
        List<String> excludeGroupings = helperService.extractGroupings(excludeGroups);

        return helperService.makeGroupings(excludeGroupings);
    }

    //returns a list of all of the groupings corresponding to the include groups in groupPaths that have the self-opted attribute
    //set in the membership
    @Override
    public List<Grouping> groupingsOptedInto(String username, List<String> groupPaths) {
        return groupingsOpted(INCLUDE, username, groupPaths);
    }

    //returns a list of all of the groupings corresponding to the exclude groups in groupPaths that have the self-opted attribute
    //set in the membership
    @Override
    public List<Grouping> groupingsOptedOutOf(String username, List<String> groupPaths) {
        return groupingsOpted(EXCLUDE, username, groupPaths);
    }

    //fetch a grouping from Grouper or the database
    @Override
    public Grouping getGrouping(String groupingPath, String ownerUsername) {
        logger.info("getGrouping; grouping: " + groupingPath + "; username: " + ownerUsername + ";");

        Grouping compositeGrouping;

        if (memberAttributeService.isOwner(groupingPath, ownerUsername) || memberAttributeService
                .isSuperuser(ownerUsername)) {
            compositeGrouping = new Grouping(groupingPath);

            String basis = groupingPath + BASIS;
            String include = groupingPath + INCLUDE;
            String exclude = groupingPath + EXCLUDE;
            String owners = groupingPath + OWNERS;

            String[] paths = { include,
                    exclude,
                    basis,
                    groupingPath,
                    owners };
            Map<String, Group> groups = getMembers(ownerUsername, Arrays.asList(paths));

            compositeGrouping = setGroupingAttributes(compositeGrouping);

            compositeGrouping.setDescription(grouperFactoryService.getDescription(groupingPath));
            compositeGrouping.setBasis(groups.get(basis));
            compositeGrouping.setExclude(groups.get(exclude));
            compositeGrouping.setInclude(groups.get(include));
            compositeGrouping.setComposite(groups.get(groupingPath));
            compositeGrouping.setOwners(groups.get(owners));

        } else {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }
        return compositeGrouping;
    }

    // Fetch a grouping from Grouper Database, but paginated based on given page + size
    // sortString sorts the database by whichever sortString category is given (e.g. "uid" will sort list by uid) before returning page
    // isAscending puts the database in ascending or descending order before returning page
    @Override
    public Grouping getPaginatedGrouping(String groupingPath, String ownerUsername, Integer page, Integer size,
            String sortString, Boolean isAscending) {
        logger.info(
                "getPaginatedGrouping; grouping: " + groupingPath + "; username: " + ownerUsername + "; page: " + page
                        + "; size: " + size + "; sortString: " + sortString + "; isAscending: " + isAscending + ";");

        if (memberAttributeService.isOwner(groupingPath, ownerUsername) || memberAttributeService
                .isSuperuser(ownerUsername)) {

            Grouping compositeGrouping = new Grouping(groupingPath);
            String basis = groupingPath + BASIS;
            String include = groupingPath + INCLUDE;
            String exclude = groupingPath + EXCLUDE;
            String owners = groupingPath + OWNERS;

            List<String> paths = new ArrayList<>();
            paths.add(include);
            paths.add(exclude);
            paths.add(basis);
            paths.add(groupingPath);
            paths.add(owners);
            Map<String, Group> groups = getPaginatedMembers(ownerUsername, paths, page, size, sortString, isAscending);
            compositeGrouping = setGroupingAttributes(compositeGrouping);

            compositeGrouping.setDescription(grouperFactoryService.getDescription(groupingPath));
            compositeGrouping.setBasis(groups.get(basis));
            compositeGrouping.setExclude(groups.get(exclude));
            compositeGrouping.setInclude(groups.get(include));
            compositeGrouping.setComposite(groups.get(groupingPath));
            compositeGrouping.setOwners(groups.get(owners));

            System.out.println("CompositeGroupingComingBack" + compositeGrouping);
            return compositeGrouping;
        } else {
            throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
        }
    }

    //get a GroupingAssignment object containing the groups that a user is in and can opt into
    @Override
    public GroupingAssignment getGroupingAssignment(String username) {
        GroupingAssignment groupingAssignment = new GroupingAssignment();
        List<String> groupPaths = getGroupPaths(username, username);

        groupingAssignment.setGroupingsIn(groupingsIn(groupPaths));
        groupingAssignment.setGroupingsOwned(groupingsOwned(groupPaths));
        groupingAssignment.setGroupingsToOptInTo(groupingsToOptInto(username, groupPaths));
        groupingAssignment.setGroupingsToOptOutOf(groupingsToOptOutOf(username, groupPaths));
        groupingAssignment.setGroupingsOptedOutOf(groupingsOptedOutOf(username, groupPaths));
        groupingAssignment.setGroupingsOptedInTo(groupingsOptedInto(username, groupPaths));

        return groupingAssignment;
    }

    /**
     * @param username: A string with the username of acting user
     * @param uid:      A string with the username of the user being acted upon
     * @return membershipAssigment: A MembershipAssignment object with the membership
     * <p>
     * Function call that makes calls to grouper in order to get the groupings a user is a member of and a list of
     * groupings a user can opt into.
     */
    @Override
    public MembershipAssignment getMembershipAssignment(String username, String uid) {

        // Creates the MembershipAssignment object.
        MembershipAssignment membershipAssignment = new MembershipAssignment();

        // Get all groups, not groupings, a user is in.
        List<String> groupPaths = getGroupPaths(username, uid);
        // Create an array list that is used to check for duplicates.
        List<String> duplicateChecker = new ArrayList<>();
        // Takes the list of groups and extracts on the groupings from them that a user is in.
        List<Grouping> memeberships = groupingsIn(groupPaths);
        // Gets the groupings that a user is an owner of.
        List<Grouping> ownerships = groupingsOwned(groupPaths);
        // Gets all the groupings the user is has been excluded from.
        List<Grouping> excludes = excludeGroups(groupPaths);
        // Creates a list for the combined list of all sub groupings: owners, exclude, and member.
        // Builds the combined list off the memberships list.
        List<Grouping> combinedGroupings = new ArrayList<>(memeberships);
        // Appends the excludes list to the combined.
        combinedGroupings.addAll(excludes);

        // Sets the groupings in and the groupings available to opt into attribute.
        membershipAssignment.setGroupingsIn(memeberships);
        membershipAssignment.setGroupingsToOptInTo(groupingsToOptInto(username, groupPaths));

        // Goes through all the groupings in memberships and adds them to appropriate attributes.
        for (Grouping grouping : memeberships) {
            // Adds the grouping name to the duplicate checker list.
            duplicateChecker.add(grouping.getPath());

            /*
                The inOwner, inBasis, inInclude, and inExlcude attributes of MembershipAssignment are hash maps that
                take in the key value pair of <String, Boolean>.

                The string is the grouping name and the boolean is the value of whether the user being acted upon (uid)
                is in the specific group i.e.:

                    membershipAssignment.addInBasis("test-grouping", memberAttributeService.isMember(grouping.getPath() + ":basis", uid));

                    This checks if the user, uid, is a part of the basis for the test-grouping. If it is, it adds the
                    key value pair <"test-grouping", true>, otherwise it adds <"test-grouping", false>.

             */
            membershipAssignment.addInOwner(grouping.getPath(), false);
            membershipAssignment.addInBasis(grouping.getPath(),
                    memberAttributeService.isMember(grouping.getPath() + ":basis", uid));

            // Checks if they are in the basis or not to determine which logic to use.
            if (membershipAssignment.isInBasis(grouping.getPath())) {
                // If they are in the basis, they can be in the exclude, include, or neither so we must check both.

                membershipAssignment.addInInclude(grouping.getPath(),
                        memberAttributeService.isMember(grouping.getPath() + ":include", uid));

                // Checks if the user is in the include, if they are in the include group add to the inExclude
                // attribute the key-value <grouping-name, false>.
                if (membershipAssignment.isInInclude(grouping.getPath())) {
                    membershipAssignment.addInExclude(grouping.getPath(), false);
                } else {
                    // If they aren't in the include, check if they are in the exclude and add the key-value pair.
                    membershipAssignment.addInExclude(grouping.getPath(),
                            memberAttributeService.isMember(grouping.getPath() + ":exclude", uid));
                }
            } else {
                // If they aren't in the basis but in the memberships groupings, they must either be in the
                // exclude or include, not both.
                if (memberAttributeService.isMember(grouping.getPath() + ":include", uid)) {
                    membershipAssignment.addInInclude(grouping.getPath(), true);
                    membershipAssignment.addInExclude(grouping.getPath(), false);
                } else {
                    membershipAssignment.addInInclude(grouping.getPath(), false);
                    membershipAssignment.addInExclude(grouping.getPath(), true);
                }
            }
        }

        // Groupings in the excludes list are never in the memberships list. They are also never in the include.
        // We only need to check for the basis.
        for (Grouping grouping : excludes) {
            duplicateChecker.add(grouping.getPath());
            membershipAssignment.addInOwner(grouping.getPath(), false);
            membershipAssignment.addInBasis(grouping.getPath(),
                    memberAttributeService.isMember(grouping.getPath() + ":basis", uid));
            membershipAssignment.addInExclude(grouping.getPath(), true);
            membershipAssignment.addInInclude(grouping.getPath(), false);

        }

        // If the person is an owner of a grouping, they could also be in basis, include, and exclude groups.
        // We must check the duplicate checker to see if the grouping is already in the combined list.
        for (Grouping grouping : ownerships) {
            // If they are not in the duplicate checker list, add the grouping to the combined list and the respective
            // MembershipAssignment attributes.
            if (!duplicateChecker.contains(grouping.getPath())) {
                combinedGroupings.add(grouping);
                membershipAssignment.addInBasis(grouping.getPath(),
                        memberAttributeService.isMember(grouping.getPath() + ":basis", uid));
                membershipAssignment.addInInclude(grouping.getPath(),
                        memberAttributeService.isMember(grouping.getPath() + ":include", uid));
                membershipAssignment.addInExclude(grouping.getPath(),
                        memberAttributeService.isMember(grouping.getPath() + ":exclude", uid));
            }

            // Update the owner attribute for all groupings in the ownerships list.
            membershipAssignment.addInOwner(grouping.getPath(), true);
        }

        // Set the combined groupings attribute.
        membershipAssignment.setCombinedGroupings(combinedGroupings);

        return membershipAssignment;
    }

    //returns an adminLists object containing the list of all admins and all groupings
    @Override
    public AdminListsHolder adminLists(String adminUsername) {
        AdminListsHolder adminListsHolder = new AdminListsHolder();
        List<Grouping> groupings;

        if (memberAttributeService.isSuperuser(adminUsername)) {

            WsGetAttributeAssignmentsResults attributeAssignmentsResults =
                    grouperFactoryService.makeWsGetAttributeAssignmentsResultsTrio(
                            ASSIGN_TYPE_GROUP,
                            TRIO);

            List<WsGroup> groups = new ArrayList<>(Arrays.asList(attributeAssignmentsResults.getWsGroups()));

            List<String> groupPaths = groups.stream().map(WsGroup::getName).collect(Collectors.toList());

            List<String> adminGrouping = new ArrayList<>(1);
            adminGrouping.add(GROUPING_ADMINS);
            Group admin = getMembers(adminUsername, adminGrouping).get(GROUPING_ADMINS);
            groupings = helperService.makeGroupings(groupPaths);
            adminListsHolder.setAdminGroup(admin);
            adminListsHolder.setAllGroupings(groupings);
            return adminListsHolder;
        }
        throw new AccessDeniedException(INSUFFICIENT_PRIVILEGES);
    }

    //returns a list of groupings corresponding to the include group or exclude group (includeOrrExclude) in groupPaths that
    //have the self-opted attribute set in the membership
    public List<Grouping> groupingsOpted(String includeOrrExclude, String username, List<String> groupPaths) {
        logger.info("groupingsOpted; includeOrrExclude: " + includeOrrExclude + "; username: " + username + ";");

        List<String> groupingsOpted = new ArrayList<>();

        List<String> groupsOpted = groupPaths.stream().filter(group -> group.endsWith(includeOrrExclude)
                && memberAttributeService.isSelfOpted(group, username)).map(helperService::parentGroupingPath)
                .collect(Collectors.toList());

        if (groupsOpted.size() > 0) {

            List<WsGetAttributeAssignmentsResults> attributeAssignmentsResults =
                    grouperFactoryService.makeWsGetAttributeAssignmentsResultsTrio(
                            ASSIGN_TYPE_GROUP,
                            TRIO,
                            groupsOpted);

            List<WsGroup> triosList = new ArrayList<>();
            for (WsGetAttributeAssignmentsResults results : attributeAssignmentsResults) {
                triosList.addAll(Arrays.asList(results.getWsGroups()));
            }

            groupingsOpted.addAll(triosList.stream().map(WsGroup::getName).collect(Collectors.toList()));
        }
        return helperService.makeGroupings(groupingsOpted);
    }

    //returns a group from grouper or the database
    @Override
    public Map<String, Group> getMembers(String ownerUsername, List<String> groupPaths) {
        logger.info("getMembers; user: " + ownerUsername + "; groups: " + groupPaths + ";");

        WsSubjectLookup lookup = grouperFactoryService.makeWsSubjectLookup(ownerUsername);
        WsGetMembersResults members = grouperFactoryService.makeWsGetMembersResults(
                SUBJECT_ATTRIBUTE_NAME_UID,
                lookup,
                groupPaths,
                null,
                null,
                null,
                null);

        Map<String, Group> groupMembers = new HashMap<>();
        if (members.getResults() != null) {
            groupMembers = makeGroups(members);
        }
        return groupMembers;
    }

    @Override
    public Map<String, Group> getPaginatedMembers(String ownerUsername, List<String> groupPaths, Integer page,
            Integer size,
            String sortString, Boolean isAscending) {
        logger.info("getPaginatedMembers; ownerUsername: " + ownerUsername + "; groups: " + groupPaths +
                "; page: " + page + "; size: " + size + "; sortString: " + sortString + "; isAscending: " + isAscending
                + ";");

        WsSubjectLookup lookup = grouperFactoryService.makeWsSubjectLookup(ownerUsername);
        WsGetMembersResults members = grouperFactoryService.makeWsGetMembersResults(
                SUBJECT_ATTRIBUTE_NAME_UID,
                lookup,
                groupPaths,
                page,
                size,
                sortString,
                isAscending);

        Map<String, Group> groupMembers = new HashMap<>();
        if (members.getResults() != null) {

            groupMembers = makeGroups(members);
        }

        return groupMembers;
    }

    //makes a group filled with members from membersResults
    @Override
    public Map<String, Group> makeGroups(WsGetMembersResults membersResults) {
        Map<String, Group> groups = new HashMap<>();
        if (membersResults.getResults().length > 0) {
            String[] attributeNames = membersResults.getSubjectAttributeNames();

            for (WsGetMembersResult result : membersResults.getResults()) {
                WsSubject[] subjects = result.getWsSubjects();
                Group group = new Group(result.getWsGroup().getName());

                if (subjects == null || subjects.length == 0) {
                    continue;
                }
                for (WsSubject subject : subjects) {
                    if (null != subject) {
                        Person personToAdd = makePerson(subject, attributeNames);
                        if (group.getPath().endsWith(BASIS) && subject.getSourceId() != null
                                && subject.getSourceId().equals(STALE_SUBJECT_ID)) {
                            personToAdd.setUsername("User Not Available.");
                        }
                        group.addMember(personToAdd);
                    }
                }
                groups.put(group.getPath(), group);
            }
        }
        // Return empty group if for any unforeseen results.
        return groups;
    }

    // Makes a person with all attributes in attributeNames.
    @Override
    public Person makePerson(WsSubject subject, String[] attributeNames) {
        if (subject == null || subject.getAttributeValues() == null) {
            return new Person();
        } else {

            Map<String, String> attributes = new HashMap<>();
            for (int i = 0; i < subject.getAttributeValues().length; i++) {
                attributes.put(attributeNames[i], subject.getAttributeValue(i));
            }
            // uhUuid is the only attribute not actually in the WsSubject attribute array.
            attributes.put(UHUUID, subject.getId());

            return new Person(attributes);
        }
    }

    // Sets the attributes of a grouping in grouper or the database to match the attributes of the supplied grouping.
    public Grouping setGroupingAttributes(Grouping grouping) {
        logger.info("setGroupingAttributes; grouping: " + grouping + ";");

        boolean isOptInOn = false;
        boolean isOptOutOn = false;

        WsGetAttributeAssignmentsResults wsGetAttributeAssignmentsResults =
                grouperFactoryService.makeWsGetAttributeAssignmentsResultsForGroup(
                        ASSIGN_TYPE_GROUP,
                        grouping.getPath());

        WsAttributeDefName[] attributeDefNames = wsGetAttributeAssignmentsResults.getWsAttributeDefNames();
        if (attributeDefNames != null && attributeDefNames.length > 0) {
            for (WsAttributeDefName defName : attributeDefNames) {
                String name = defName.getName();
                if (name.equals(OPT_IN)) {
                    isOptInOn = true;
                } else if (name.equals(OPT_OUT)) {
                    isOptOutOn = true;
                }
            }
        }

        grouping.setOptInOn(isOptInOn);
        grouping.setOptOutOn(isOptOutOn);

        // Set the sync destinations.
        List<SyncDestination> syncDestinations = groupAttributeService.getSyncDestinations(grouping);
        grouping.setSyncDestinations(syncDestinations);

        return grouping;
    }

    // Returns the list of groups that the user is in, searching by username or uhUuid.
    @Override
    public List<String> getGroupPaths(String ownerUsername, String username) {
        logger.info("getGroupPaths; username: " + username + ";");

        if (ownerUsername.equals(username) || memberAttributeService.isSuperuser(ownerUsername)) {
            WsStemLookup stemLookup = grouperFactoryService.makeWsStemLookup(STEM);
            WsGetGroupsResults wsGetGroupsResults;

            wsGetGroupsResults = grouperFactoryService.makeWsGetGroupsResults(
                    username,
                    stemLookup,
                    StemScope.ALL_IN_SUBTREE
            );

            WsGetGroupsResult groupResults = wsGetGroupsResults.getResults()[0];

            List<WsGroup> groups = new ArrayList<>();

            if (groupResults.getWsGroups() != null) {
                groups = new ArrayList<>(Arrays.asList(groupResults.getWsGroups()));
            }

            return extractGroupPaths(groups);

        } else {
            return new ArrayList<>();
        }
    }

    @Override
    //take a list of WsGroups ans return a list of the paths for all of those groups
    public List<String> extractGroupPaths(List<WsGroup> groups) {
        Set<String> names = new LinkedHashSet<>();
        if (groups != null) {
            names = groups
                    .parallelStream()
                    .map(WsGroup::getName)
                    .collect(Collectors.toSet());

        }
        return names.stream().collect(Collectors.toList());
    }

    //returns the list of groupings that the user is allowed to opt-in to
    public List<Grouping> groupingsToOptInto(String optInUsername, List<String> groupPaths) {
        logger.info("groupingsToOptInto; username: " + optInUsername + "; groupPaths : " + groupPaths + ";");

        List<String> trios = new ArrayList<>();
        List<String> opts = new ArrayList<>();
        List<String> excludes = groupPaths
                .stream()
                .map(group -> group + EXCLUDE)
                .collect(Collectors.toList());

        WsGetAttributeAssignmentsResults assignmentsResults =
                grouperFactoryService.makeWsGetAttributeAssignmentsResultsTrio(
                        ASSIGN_TYPE_GROUP,
                        TRIO,
                        OPT_IN);

        if (assignmentsResults.getWsAttributeAssigns() != null) {
            for (WsAttributeAssign assign : assignmentsResults.getWsAttributeAssigns()) {
                if (assign.getAttributeDefNameName() != null) {
                    if (assign.getAttributeDefNameName().equals(TRIO)) {
                        trios.add(assign.getOwnerGroupName());
                    } else if (assign.getAttributeDefNameName().equals(OPT_IN)) {
                        opts.add(assign.getOwnerGroupName());
                    }
                }
            }

            //opts intersection trios
            opts.retainAll(trios);
            //excludes intersection opts
            excludes.retainAll(opts);
            //opts - (opts intersection groupPaths)
            opts.removeAll(groupPaths);
            //opts union excludes
            opts.addAll(excludes);

        }

        //get rid of duplicates
        List<String> groups = new ArrayList<>(new HashSet<>(opts));
        return helperService.makeGroupings(groups);
    }

    //returns a list of groupings that the user is allowed to opt-out of
    public List<Grouping> groupingsToOptOutOf(String optOutUsername, List<String> groupPaths) {
        logger.info("groupingsToOptOutOf; username: " + optOutUsername + "; groupPaths: " + groupPaths + ";");

        List<String> trios = new ArrayList<>();
        List<String> opts = new ArrayList<>();
        List<WsAttributeAssign> attributeAssigns = new ArrayList<>();

        List<WsGetAttributeAssignmentsResults> assignmentsResults =
                grouperFactoryService.makeWsGetAttributeAssignmentsResultsTrio(
                        ASSIGN_TYPE_GROUP,
                        TRIO,
                        OPT_OUT,
                        groupPaths);

        assignmentsResults
                .stream()
                .filter(results -> results.getWsAttributeAssigns() != null)
                .forEach(results -> attributeAssigns.addAll(Arrays.asList(results.getWsAttributeAssigns())));

        if (attributeAssigns.size() > 0) {
            attributeAssigns.stream().filter(assign -> assign.getAttributeDefNameName() != null).forEach(assign -> {
                if (assign.getAttributeDefNameName().equals(TRIO)) {
                    trios.add(assign.getOwnerGroupName());
                } else if (assign.getAttributeDefNameName().equals(OPT_OUT)) {
                    opts.add(assign.getOwnerGroupName());
                }
            });

            opts.retainAll(trios);
        }

        return helperService.makeGroupings(opts);
    }

}
