package org.ovirt.engine.ui.uicommonweb.models.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.aaa.DirectoryGroup;
import org.ovirt.engine.core.aaa.DirectoryUser;
import org.ovirt.engine.core.aaa.ProfileEntry;
import org.ovirt.engine.core.common.businessentities.Queryable;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.businessentities.comparators.NameableComparator;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.QueryParametersBase;
import org.ovirt.engine.core.common.queries.QueryReturnValue;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.auth.ApplicationGuids;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class AdElementListModel extends SearchableListModel<Object, EntityModel<DbUser>> {

    public enum AdSearchType {
        USER,
        GROUP,
        EVERYONE,
        MY_GROUPS;
    }

    private UICommand privateSearchMyGroupsCommand;

    public UICommand getSearchMyGroupsCommand() {
        return privateSearchMyGroupsCommand;
    }

    private void setSearchMyGroupsCommand(UICommand value) {
        privateSearchMyGroupsCommand = value;
    }

    private EntityModel<Boolean> searchInProgress;

    public EntityModel<Boolean> getSearchInProgress() {
        return searchInProgress;
    }

    public void setSearchInProgress(EntityModel<Boolean> searchInProgress) {
        this.searchInProgress = searchInProgress;
    }

    private Iterable<DbUser> privateExcludeItems;

    private Map<String, List<String>> namespacesMap = new HashMap<>();

    public Iterable<DbUser> getExcludeItems() {
        return privateExcludeItems;
    }

    public void setExcludeItems(Iterable<DbUser> value) {
        privateExcludeItems = value;
    }

    private ListModel<ProfileEntry> privateProfile;

    public ListModel<ProfileEntry> getProfile() {
        return privateProfile;
    }

    private void setProfile(ListModel<ProfileEntry> value) {
        privateProfile = value;
    }

    private ListModel<String> privateNamespace;

    public void setNamespace(ListModel<String> value) {
        privateNamespace = value;
    }

    public ListModel<String> getNamespace() {
        return privateNamespace;
    }

    private ListModel<Role> privateRole;

    public ListModel<Role> getRole() {
        return privateRole;
    }

    private void setRole(ListModel<Role> value) {
        privateRole = value;
    }

    private EntityModel<Boolean> privateSelectAll;

    public EntityModel<Boolean> getSelectAll() {
        return privateSelectAll;
    }

    public void setSelectAll(EntityModel<Boolean> value) {
        privateSelectAll = value;
    }

    // This is required for the webadmin.
    private EntityModel<Boolean> privateIsRoleListHiddenModel;

    public EntityModel<Boolean> getIsRoleListHiddenModel() {
        return privateIsRoleListHiddenModel;
    }

    private void setIsRoleListHiddenModel(EntityModel<Boolean> value) {
        privateIsRoleListHiddenModel = value;
    }

    private EntityModel<Boolean> privateIsEveryoneSelectionHidden;

    public EntityModel<Boolean> getIsEveryoneSelectionHidden() {
        return privateIsEveryoneSelectionHidden;
    }

    private void setIsEveryoneSelectionHidden(EntityModel<Boolean> value) {
        privateIsEveryoneSelectionHidden = value;
    }

    private EntityModel<Boolean> privateIsMyGroupsSelectionHidden;

    public EntityModel<Boolean> getIsMyGroupsSelectionHidden() {
        return privateIsMyGroupsSelectionHidden;
    }

    private void setIsMyGroupsSelectionHidden(EntityModel<Boolean> value) {
        privateIsMyGroupsSelectionHidden = value;
    }

    private boolean isRoleListHidden;

    public boolean getIsRoleListHidden() {
        return searchType == AdSearchType.EVERYONE || searchType == AdSearchType.MY_GROUPS;
    }

    public void setIsRoleListHidden(boolean value) {
        if (isRoleListHidden != value) {
            isRoleListHidden = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsRoleListHidden")); //$NON-NLS-1$
        }
        if (getIsRoleListHiddenModel() != null) {
            getIsRoleListHiddenModel().setEntity(value);
        }
    }

    private AdSearchType searchType;

    public AdSearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(AdSearchType value) {
        if (searchType != value) {
            searchType = value;
            onPropertyChanged(new PropertyChangedEventArgs("AdSearchType")); //$NON-NLS-1$
        }
    }

    private List<ProfileEntry> profileEntries;

    public AdElementListModel() {
        setSearchMyGroupsCommand(new UICommand("SearchMyGroups", this)); //$NON-NLS-1$
        setRole(new ListModel<Role>());
        setProfile(new ListModel<ProfileEntry>());
        setNamespace(new ListModel<String>());

        setSelectAll(new EntityModel<Boolean>());
        getSelectAll().setEntity(false);
        getSelectAll().getEntityChangedEvent().addListener(this);

        setIsRoleListHiddenModel(new EntityModel<Boolean>());
        getIsRoleListHiddenModel().setEntity(false);

        setIsEveryoneSelectionHidden(new EntityModel<Boolean>());
        getIsEveryoneSelectionHidden().setEntity(false);

        setIsMyGroupsSelectionHidden(new EntityModel<Boolean>());
        getIsMyGroupsSelectionHidden().setEntity(false);

        setSearchInProgress(new EntityModel<Boolean>());
        getSearchInProgress().setEntity(false);

        searchType = AdSearchType.USER;

        setIsTimerDisabled(true);

        AsyncDataProvider.getInstance().getAAAProfilesEntriesList(new AsyncQuery<>(result -> {
            setProfileEntries(result);
            AsyncDataProvider.getInstance().getAAANamespaces(new AsyncQuery<>(r -> {
                namespacesMap = r;
                populateProfiles(getProfileEntries());
            }));
        }));

        AsyncDataProvider.getInstance().getRoleList(new AsyncQuery<>(result -> populateRoles(result)));
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getSearchMyGroupsCommand()) {
            searchMyGroups();
        }
    }

    public void searchMyGroups() {
        getSearchInProgress().setEntity(true);
        AsyncQuery<QueryReturnValue> asyncQuery = new AsyncQuery<>(queryReturnValue -> {
            if (handleQueryError(queryReturnValue)) {
                return;
            }

            HashSet<String> excludeUsers = new HashSet<>();
            if (getExcludeItems() != null) {
                for (DbUser item : getExcludeItems()) {
                    excludeUsers.add(item.getExternalId());
                }
            }
            setgroups(new ArrayList<EntityModel<DbUser>>());
            addGroupsToModel(queryReturnValue, excludeUsers);
            setusers(new ArrayList<EntityModel<DbUser>>());
            onUserAndAdGroupsLoaded();
        });
        asyncQuery.setHandleFailure(true);

        Frontend.getInstance()
                .runQuery(QueryType.GetDirectoryGroupsForUser, new QueryParametersBase(), asyncQuery);
    }

    protected void populateProfiles(List<ProfileEntry> profiles) {
        getProfile().setItems(profiles);
        getProfile().setSelectedItem(Linq.firstOrNull(getProfile().getItems()));
    }

    public void populateNamespaces() {
        if (namespacesMap != null) {
            getNamespace().setItems(getAuthzNamespaces());
            getNamespace().setSelectedItem(Linq.firstOrNull(getNamespace().getItems()));

        }
    }

    protected void setProfileEntries(List<ProfileEntry> value) {
        profileEntries = value;
    }

    protected List<ProfileEntry> getProfileEntries() {
        return profileEntries;

    }

    protected void populateRoles(List<Role> roles){
        Role selectedRole = null;
        List<Role> rolesToPopulate = new ArrayList<>();

        for (Role role : roles) {

            if (role.getId() != null) {
             // ignore CONSUME_QUOTA_ROLE in UI
                if (!role.getId().equals(ApplicationGuids.quotaConsumer.asGuid())) {
                    rolesToPopulate.add(role);
                }
                //select engine user if it exists
                if (role.getId().equals(ApplicationGuids.engineUser.asGuid())) {
                    selectedRole = role;
                }
            }
        }

        Collections.sort(rolesToPopulate, new NameableComparator());

        getRole().setItems(rolesToPopulate);
        if (selectedRole != null) {
            getRole().setSelectedItem(selectedRole);
        } else if (rolesToPopulate.size() > 0){
            //if engine user does not exist, pick the first on the list
            getRole().setSelectedItem(rolesToPopulate.get(0));
        }
    }

    @Override
    protected void syncSearch() {
        // allow only a single user lookup at a time
        if (getSearchInProgress().getEntity()) {
            return;
        }
        getSearchInProgress().setEntity(true);

        super.syncSearch();
        // var exclude = ExcludeItems != null ? ExcludeItems.Cast<DbUser>() : new List<DbUser>();

        if (this.searchType == AdSearchType.USER) {
            syncSearchUsers();
        } else if (this.searchType == AdSearchType.GROUP) {
            syncSearchGroups();
        }
    }

    private void syncSearchUsers() {
        AsyncQuery<QueryReturnValue> asyncQuery = new AsyncQuery<>(queryReturnValue -> {
            if (handleQueryError(queryReturnValue)) {
                return;
            }

            setusers(new ArrayList<EntityModel<DbUser>>());
            addUsersToModel(queryReturnValue, getExcludeUsers());
            onAdUsersLoaded();
        });
        asyncQuery.setHandleFailure(true);

        findUsers("allnames=" //$NON-NLS-1$
                + (StringHelper.isNullOrEmpty(getSearchString()) ? "*" : getSearchString()), //$NON-NLS-1$
                asyncQuery);
    }

    private void syncSearchGroups() {
        AsyncQuery<QueryReturnValue> asyncQuery = new AsyncQuery<>(queryReturnValue -> {
            if (handleQueryError(queryReturnValue)) {
                return;
            }

            HashSet<String> excludeUsers = new HashSet<>();
            if (getExcludeItems() != null) {
                for (DbUser item : getExcludeItems()) {
                    excludeUsers.add(item.getExternalId());
                }
            }
            setgroups(new ArrayList<EntityModel<DbUser>>());
            addGroupsToModel(queryReturnValue, excludeUsers);
            onAdGroupsLoaded();
        });
        asyncQuery.setHandleFailure(true);

        findGroups("name=" + (StringHelper.isNullOrEmpty(getSearchString()) ? "*" : getSearchString()), //$NON-NLS-1$ //$NON-NLS-2$
                asyncQuery);
    }

    protected void addUsersToModel(QueryReturnValue returnValue, Set<String> excludeUsers) {
        for (Queryable item : (List<Queryable>) returnValue.getReturnValue()) {
            DirectoryUser a = (DirectoryUser) item;
            if (!excludeUsers.contains(a.getId())) {
                EntityModel<DbUser> tempVar2 = new EntityModel<>();
                tempVar2.setEntity(new DbUser(a));
                getusers().add(tempVar2);
            }
        }
    }

    protected void addGroupsToModel(QueryReturnValue returnValue, Set<String> excludeUsers) {
        for (Queryable item : (Collection<Queryable>) returnValue.getReturnValue()) {
            DirectoryGroup a = (DirectoryGroup) item;
            if (!excludeUsers.contains(a.getId())) {
                // TODO: This is a hack, we should either use DbGroup or reimplement user/group representation in GWT
                DbUser group = new DbUser();
                group.setExternalId(a.getId());
                group.setFirstName(a.getName());
                group.setLastName(""); //$NON-NLS-1$
                // TODO: Due to group -> DbUser mapping hack we have to use note to represent display name of group
                group.setNote(a.getDisplayName());
                group.setDomain(a.getDirectoryName());
                group.setNamespace(a.getNamespace());

                EntityModel<DbUser> groupEntity = new EntityModel<>();
                groupEntity.setEntity(group);
                getgroups().add(groupEntity);
            }
        }
    }
    protected Set<String> getExcludeUsers() {
        Set<String> excludeUsers = new HashSet<>();
        if (getExcludeItems() != null) {
            for (DbUser item : getExcludeItems()) {
                excludeUsers.add(item.getExternalId());
            }
        }
        return excludeUsers;
    }

    protected void findGroups(String searchString, AsyncQuery query) {
        Frontend.getInstance()
                .runQuery(QueryType.Search,
                        new SearchParameters("ADGROUP@" + getProfile().getSelectedItem().getAuthz() + ":" + getNamespace().getSelectedItem() + ": " + searchString, SearchType.DirectoryGroup), query); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    protected void findUsers(String searchString, AsyncQuery query) {
        Frontend.getInstance()
                .runQuery(QueryType.Search,
                        new SearchParameters("ADUSER@" + getProfile().getSelectedItem().getAuthz() + ":" + getNamespace().getSelectedItem() + ": " + searchString, SearchType.DirectoryUser), query); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    protected void onAdUsersLoaded() {
        onAdItemsLoaded(getusers());
    }

    protected void onAdGroupsLoaded() {
        onAdItemsLoaded(getgroups());
    }

    private void onAdItemsLoaded(List<EntityModel<DbUser>> userOrGroups) {
        getSearchInProgress().setEntity(false);
        List<EntityModel<DbUser>> items = new ArrayList<>();
        items.addAll(userOrGroups);
        getSelectAll().setEntity(false);
        setItems(items);
        setIsEmpty(items.isEmpty());
    }

    protected void onUserAndAdGroupsLoaded() {
        if (getusers() != null && getgroups() != null) {
            getSearchInProgress().setEntity(false);

            List<EntityModel<DbUser>> items = new ArrayList<>();
            items.addAll(getusers());
            items.addAll(getgroups());
            getSelectAll().setEntity(false);
            setItems(items);
            setusers(null);
            setgroups(getusers());

            setIsEmpty(items.isEmpty());
        }
    }

    private ArrayList<EntityModel<DbUser>> privateusers;

    public ArrayList<EntityModel<DbUser>> getusers() {
        return privateusers;
    }

    public void setusers(ArrayList<EntityModel<DbUser>> value) {
        privateusers = value;
    }

    private ArrayList<EntityModel<DbUser>> privategroups;

    public ArrayList<EntityModel<DbUser>> getgroups() {
        return privategroups;
    }

    public void setgroups(ArrayList<EntityModel<DbUser>> value) {
        privategroups = value;
    }

    @Override
    public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
        super.eventRaised(ev, sender, args);

        if (sender == getSelectAll()) {
            if (getItems() == null) {
                return;
            }
            boolean selectAll = getSelectAll().getEntity();
            for (EntityModel<DbUser> item : getItems()) {
                item.setIsSelected(selectAll);
            }
        }
    }

    public boolean availableNamespaces() {
        return getAuthzNamespaces() != null && !getAuthzNamespaces().isEmpty();
    }

    @Override
    protected String getListName() {
        return "AdElementListModel"; //$NON-NLS-1$
    }

    /**
     * Handle error message in case of a query failure
     * @param returnValue query return value
     * @return true if a query failure has occurred
     */
    private boolean handleQueryError(QueryReturnValue returnValue) {
        setMessage(null);
        if (!returnValue.getSucceeded()) {
            setMessage(Frontend.getInstance().getAppErrorsTranslator()
                    .translateErrorTextSingle(returnValue.getExceptionString()));
            getSearchInProgress().setEntity(false);

            return true;
        }

        return false;
    }

    private List<String> getAuthzNamespaces() {
        ProfileEntry profileEntry = getProfile().getSelectedItem();
        return profileEntry != null ? namespacesMap.get(profileEntry.getAuthz()) : Collections.<String> emptyList();
    }

}
