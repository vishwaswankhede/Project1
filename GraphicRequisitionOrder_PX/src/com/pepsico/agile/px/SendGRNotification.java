package com.pepsico.agile.px;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.IUser;
import com.agile.api.IUserGroup;
import com.agile.api.ItemConstants;
import com.agile.api.UserGroupConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.px.util.PropertiesLoader;

public class SendGRNotification implements IEventAction {
	private Logger log = Logger.getLogger(SendGRNotification.class.getName());
	ArrayList suppliersList = null;

	@Override
	public EventActionResult doAction(IAgileSession session, INode node, IEventInfo eventInfo) {

		// Load Property file
		PropertiesLoader.loadResource("CornerStone");
		DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
		log.info("**Inside SendGRNotification PX**");
		ActionResult actionResult = null;
		suppliersList = new ArrayList();

		try {
			IWFChangeStatusEventInfo info = (IWFChangeStatusEventInfo) eventInfo;
			IChange grco = (IChange) info.getDataObject();
			log.info("GRCO " + grco + " is moved to " + grco.getStatus().toString());
			ArrayList aiList = getAllAffectedItems(grco);
			getSuppliersToBeNotified(aiList, grco, session);
			Iterator itr = suppliersList.iterator();
			log.info("Iterating the suppliers group of each affected item to send the notification");
			while (itr.hasNext()) {
				String groupName = itr.next().toString();
				ArrayList users = new ArrayList();
				users = getAllUsers(session, groupName);
				session.sendNotification(grco, PropertiesLoader.getProperty("NOTIFICATION_GR_CANCEL_OR_COMPLETE"),
						users, true, "");
				log.info("Notification is sent to " + groupName + " Suppliers");
				actionResult = new ActionResult(ActionResult.STRING, "Notification is sent to Suppliers");
			}
		} catch (APIException ex) {
			log.error(ex);
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		} catch (Exception ex) {
			log.error(ex);
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		}
		return new EventActionResult(eventInfo, actionResult);
	}

	public ArrayList getAllAffectedItems(IChange grco) throws APIException {
		ArrayList aiList = new ArrayList();
		log.info("Loading all affected items of type Graphic Requisition");
		ITable aiTable = grco.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator itr = aiTable.iterator();
		log.info("AI Table Size " + aiTable.size());
		while (itr.hasNext()) {
			IRow row = (IRow) itr.next();
			if (row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString()
					.equalsIgnoreCase(PropertiesLoader.getProperty("SUBCLASS_GRAPHIC_REQUISITION")))
				aiList.add(row.getReferent());
		}
		return aiList;
	}

	public void getSuppliersToBeNotified(ArrayList aiList, IChange grco, IAgileSession session) throws APIException {
		log.info("Getting suppliers associated with each Graphic Requisition in the GRCO " + grco.getName());
		Iterator itr = aiList.iterator();
		while (itr.hasNext()) {
			IItem item = (IItem) itr.next();
			IUserGroup userGroup = getAttributeValueFromDataObj(item, ItemConstants.TABLE_REDLINEPAGETHREE, grco,
					"list25", true, session);
			if (userGroup != null && !suppliersList.contains(userGroup)) {
				suppliersList.add(userGroup);
			}
		}
	}

	private IUserGroup getAttributeValueFromDataObj(final IItem dataObj, final Integer attrTableID,
			final IChange changeObj, final String attrName, final boolean isChangeControlled, IAgileSession session)
					throws APIException {
		log.info("Getting supplier group associated with " + dataObj);
		IUserGroup attrValue = null;
		// If change controlled get from redline table
		dataObj.setRevision(changeObj);
		if (isChangeControlled) {
			final Iterator<IRow> p3Attrs = dataObj.getTable(attrTableID).iterator();
			IRow p3Row = p3Attrs.next();
			String attrValue1 = p3Row.getValue("Page Three.Supplier/Printer Name (New)").toString();
			attrValue = (IUserGroup) session.getObject(UserGroupConstants.CLASS_USER_GROUPS_CLASS, attrValue1);
		} else {
			// Else get directly
			attrValue = (IUserGroup) dataObj.getValue("Page Three.Supplier/Printer Name (New)");
		}
		log.info("Supplier is :"+attrValue.getName());
		return attrValue;
	}	

	public ArrayList getAllUsers(IAgileSession session, String userGroupName) throws APIException {
		log.info("Loading users of group : " + userGroupName);
		ArrayList users = new ArrayList();
		IUserGroup userGroup = (IUserGroup) session.getObject(UserGroupConstants.CLASS_USER_GROUP_BASE_CLASS,
				userGroupName);
		ITable userTab = userGroup.getTable(UserGroupConstants.TABLE_USERS);
		Iterator itr = userTab.iterator();
		while (itr.hasNext()) {
			IRow row = (IRow) itr.next();
			IUser user = (IUser) row.getReferent();
			log.info("user " + user.getName());
			users.add(user);
		}
		return users;
	}

}
