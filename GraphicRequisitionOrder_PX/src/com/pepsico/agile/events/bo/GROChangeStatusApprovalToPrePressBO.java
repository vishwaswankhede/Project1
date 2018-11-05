/**
 * 
 */
package com.pepsico.agile.events.bo;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.IUser;
import com.agile.api.ItemConstants;
import com.pepsico.agile.px.util.PropertiesLoader;

/**
 * @author 09163426
 *
 */
public class GROChangeStatusApprovalToPrePressBO {
	private static Logger log = Logger.getLogger(GROChangeStatusApprovalToPrePressBO.class.getName());
	public boolean flag = false;
    private StringBuilder strBuilder = new StringBuilder();
	
	public GROChangeStatusApprovalToPrePressBO(){
		PropertiesLoader.loadResource("CornerStone");
		DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
	}
	
	/***
	 * Get affected item from GRO and updated the PVSpec attribute on TradeGTIN with value obtained from GR
	 * @param graphicRequisitionChangeOrder
	 * @param agileSession
	 * @return
	 * @throws APIException
	 */
	@SuppressWarnings("unchecked")
	public String getGROAffectedItem(IChange graphicRequisitionChangeOrder, IAgileSession agileSession) throws APIException{
		String message = "";
		String strTadeGTIN = "";
		
		IItem grObject = null;
		log.info("GR Order : "+graphicRequisitionChangeOrder);
		ITable grAffectedItemTable = graphicRequisitionChangeOrder.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		Iterator<IRow> affectedRow = grAffectedItemTable.iterator();
		while (affectedRow.hasNext()) {
			IRow iRow = (IRow) affectedRow.next();
			grObject = (IItem) iRow.getReferent();
			log.info("GR object from : "+grObject+" from GRO "+graphicRequisitionChangeOrder+" afected item tab");
			if(checkGRHasPendingChange(grObject)){	
				IItem tradeGTIN = getTradeGTINFromGR(grObject, graphicRequisitionChangeOrder, getBaseIdOrAPIName(PropertiesLoader.getProperty("ATTRID_GTIN_ON_GR")), agileSession);
				IItem pkgSpecObj = getPkgSpecObjFromGR(grObject, graphicRequisitionChangeOrder, getBaseIdOrAPIName(PropertiesLoader.getProperty("GR_ID_PACKAGE_SPECIFICATION")), agileSession);
				List<IUser> originatorGROList = getOriginatorOnGR(graphicRequisitionChangeOrder, agileSession);
				if(tradeGTIN != null ){
					if(!hasTradeGTINPendingChange(tradeGTIN, agileSession)){
						if(isLifecyclePhaseInPrelimnary(tradeGTIN)){
							if(isPVSpecAttributeBlank(tradeGTIN)){
								tradeGTIN.setValue(getBaseIdOrAPIName(PropertiesLoader.getProperty("TRADE_GTIN_P3_PV_SPEC")), pkgSpecObj);
								//Creating Pxadmin session to log the history on Preliminary TGTIN: defect 2708
								tradeGTIN = (IItem)agileSession.getObject(IItem.OBJECT_TYPE, tradeGTIN.getName());
								tradeGTIN.logAction("PV Spec attribute updated to "+pkgSpecObj+" on TradeGTIN "+tradeGTIN+" associated to the GR "+grObject+" and GRCO "+graphicRequisitionChangeOrder);
								log.info("PV Spec attribute updated to "+pkgSpecObj+" on TradeGTIN "+tradeGTIN+" associated to the GR "+grObject+" and GRCO "+graphicRequisitionChangeOrder);
								message = MessageFormat.format("PV Spec attribute updated to {0} on TradeGTIN {1} associated to the GR {2}.", new Object[]{pkgSpecObj, tradeGTIN, grObject});
								strBuilder.append(message);
								setFlag(true);
							}else{
								log.warn("PV Spec attribute on TradeGTIN "+tradeGTIN+" cannot be updated, since it is not blank.");
								message= MessageFormat.format("PV Spec attribute on TradeGTIN {0} cannot be updated, since it is not blank.", new Object[]{tradeGTIN});
								strBuilder.append(message);
								agileSession.sendNotification(graphicRequisitionChangeOrder, PropertiesLoader.getProperty("PV_SPEC_NOT_UPDATED_NOTIFICATION"), originatorGROList, false, "");
								setFlag(false);
							}
						}else{
							log.warn("Trade GTIN is not in Prelimnary lifecycle status. Hence, "+pkgSpecObj+" is not updated to 'PV Spec' attribute");
							message = MessageFormat.format("Trade GTIN is not in Prelimnary lifecycle status. Hence, {0} is not updated to 'PV Spec' attribute.", new Object[]{tradeGTIN});
							strBuilder.append(message);
							agileSession.sendNotification(graphicRequisitionChangeOrder, PropertiesLoader.getProperty("TRADE_GTIN_NOT_IN_PRELIM"), originatorGROList, false, "");
							setFlag(false);
						}
					}else{
						log.warn("TradeGTIN "+strTadeGTIN+" has a pending change. Hence, "+pkgSpecObj+" is not updated to 'PV Spec' attribute");
						message = MessageFormat.format("TradeGTIN {0} has a pending change. Hence, {1} is not updated to 'PV Spec' attribute.", new Object[]{tradeGTIN, pkgSpecObj});
						strBuilder.append(message);
						agileSession.sendNotification(graphicRequisitionChangeOrder, PropertiesLoader.getProperty("TRADE_GTIN_HAS_PEDNING_CHANGE"), originatorGROList, false, "");
						setFlag(false);
					}
				}
			}
		}
		return strBuilder.toString();
	}
	
	
	/***
	 * Get Originator on the GRO to send notification
	 * @param graphicRequisitionChangeOrder
	 * @param agileSession
	 * @return
	 * @throws APIException
	 */
	private List<IUser> getOriginatorOnGR(IChange graphicRequisitionChangeOrder, IAgileSession agileSession) throws APIException {
		String strUserOnGRO = "";
		String strUser = "";
		List<IUser> list = null;
		if(graphicRequisitionChangeOrder != null && !graphicRequisitionChangeOrder.equals("")){
			strUserOnGRO = graphicRequisitionChangeOrder.getValue(getBaseIdOrAPIName(PropertiesLoader.getProperty("GRO_ORIGINATOR"))).toString();
			log.info("GR Originator : "+strUserOnGRO);
			strUser = strUserOnGRO.substring(strUserOnGRO.indexOf("(")+1, strUserOnGRO.indexOf(")"));
			log.info("Originator : "+strUser);
			IUser userOriginator = (IUser) agileSession.getObject(IUser.OBJECT_TYPE, strUser);
			log.info("Originator on GRO "+userOriginator);
			list = new ArrayList<IUser>();
	        list.add(userOriginator);
		}
		return list;		
	}

	/***
	 * Get Packaging Spec ID object from GR
	 * @param grObject
	 * @param graphicRequisitionChangeOrder
	 * @param obj
	 * @param agileSession
	 * @return
	 * @throws APIException
	 */
	private IItem getPkgSpecObjFromGR(IItem grObject,
			IChange graphicRequisitionChangeOrder, Object obj,
			IAgileSession agileSession) throws APIException {
		String strPkgSpecID = getValueFromObjPageThree(grObject, graphicRequisitionChangeOrder, getBaseIdOrAPIName(PropertiesLoader.getProperty("GR_ID_PACKAGE_SPECIFICATION")));
		log.info("Packaging Specification ID on GR : "+strPkgSpecID);
		IItem pkgSpecObj = (IItem) agileSession.getObject(IItem.OBJECT_TYPE, strPkgSpecID);
		return pkgSpecObj;
	}

	/***
	 * Get Trade GTIN object from GR
	 * @param grObject
	 * @param graphicRequisitionChangeOrder
	 * @param obj
	 * @param agileSession
	 * @return
	 * @throws APIException
	 */
	private IItem getTradeGTINFromGR(IItem grObject,
		IChange graphicRequisitionChangeOrder, Object obj, IAgileSession agileSession) throws APIException {
		String strTadeGTIN = getValueFromObjPageThree(grObject, graphicRequisitionChangeOrder, obj);
		log.info("Trade GTIN number on GR : "+strTadeGTIN);		
		IItem tradeGTIN = (IItem) agileSession.getObject(IItem.OBJECT_TYPE, strTadeGTIN);
		return tradeGTIN;
	}

	/***
	 * Check if the PV Spec attribute on TradeGTIn is blank
	 * @param tradeGTIN
	 * @return
	 * @throws APIException
	 */
	private boolean isPVSpecAttributeBlank(IItem tradeGTIN) throws APIException {
		boolean isPVSpecAttr = false;
		if(tradeGTIN != null && !tradeGTIN.equals("")){
			String strPVSpec = tradeGTIN.getValue(getBaseIdOrAPIName(PropertiesLoader.getProperty("TRADE_GTIN_P3_PV_SPEC"))).toString();
			log.info("PV Spec attribute value '"+strPVSpec+"' on Trade GTIN "+tradeGTIN);
			if(strPVSpec.equalsIgnoreCase("")){
				log.info("PV Spec attribute value on TradeGTIN is blank");
				isPVSpecAttr = true;			
			}
		}
		return isPVSpecAttr;
	}

	/***
	 * Check if Trade GTIN is in Preliminary lifecycle phase
	 * @param tradeGTIN
	 * @return
	 * @throws APIException
	 */
	private boolean isLifecyclePhaseInPrelimnary(IItem tradeGTIN) throws APIException {
		boolean isLifecyclePhase = false;
		if(tradeGTIN != null && !tradeGTIN.equals("")){
			String lifecyclePhase = tradeGTIN.getValue(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE).toString();
			log.info("Trade GTIN "+tradeGTIN+" current lifecycle phase is "+lifecyclePhase);
			if(lifecyclePhase.equalsIgnoreCase(PropertiesLoader.getProperty("TRADE_GTIN_PRELIM_LFC"))){
					log.info("Trade GTIN is in Prelimnary status");
				isLifecyclePhase = true;
			}
		}		
		return isLifecyclePhase;
	}

	/***
	 * Check if TradeGTIN has pending change
	 * @param strTadeGTIN
	 * @param agileSession
	 * @return
	 * @throws APIException
	 */
	private boolean hasTradeGTINPendingChange(IItem tradeGTIN, IAgileSession agileSession) throws APIException {
		boolean hasPendingChange = false;
		if(tradeGTIN != null && !tradeGTIN.equals("")){
			log.info("Trade GTIN obtained from GR : "+tradeGTIN);
			hasPendingChange = tradeGTIN.isFlagSet(ItemConstants.FLAG_HAS_PENDING_ECO);
			log.info("TradeGTIN has pending change ? : "+hasPendingChange);
		}	
		return hasPendingChange;
	}	

	/***
	 * Get redlined value from Page Three on the Item Object
	 * @param itemObj
	 * @param affectedChange
	 * @param attrName
	 * @return
	 * @throws APIException
	 */
	@SuppressWarnings("unchecked")
	private String getValueFromObjPageThree(final IItem itemObj, final IChange affectedChange, final Object attrName) throws APIException
	{
		String returnValue = "";
		final IChange changeObj = getCurrentChangeRevision(itemObj, affectedChange);
		if (changeObj != null)
		{
			itemObj.setRevision(changeObj);
			ITable p3RedlinedTable = itemObj.getTable(ItemConstants.TABLE_REDLINEPAGETHREE);
			Iterator<IRow> p3Attrs = p3RedlinedTable.iterator();
			IRow p3Row = p3Attrs.next();
			log.info("Attribute name --- "+attrName);
			ICell p3RowCell = p3Row.getCell(attrName);
			if(p3RowCell.getValue() != null){
				returnValue = p3RowCell.getValue().toString();
				log.info("Redlined value (NEW) : "+returnValue);
			}else{
				returnValue = p3RowCell.getOldValue().toString();
				log.info("Redlined value (OLD) : "+returnValue);
			}
		}
		else
		{
			if(itemObj.getValue(attrName)!=null){
				returnValue = itemObj.getValue(attrName).toString();
			}
		}
		return returnValue;
	}
	
	/***
	 * Get current change revision
	 * @param itemObj
	 * @param affectedChange
	 * @return
	 * @throws APIException
	 */
	@SuppressWarnings("unchecked")
	private IChange getCurrentChangeRevision(final IItem itemObj, final IChange affectedChange) throws APIException{
		IChange changeNumber = null;
		if (affectedChange != null){
			final ITable pendingChanges = itemObj.getTable(ItemConstants.TABLE_PENDINGCHANGES);
			Iterator<IRow> iterator = pendingChanges.iterator();
			while (iterator.hasNext()){
				IRow row = iterator.next();
				if (row.getValue(ItemConstants.ATT_PENDING_CHANGES_NUMBER).toString().equalsIgnoreCase(affectedChange.getName()))
				{
					changeNumber = affectedChange;
					log.info("Change Number : "+changeNumber);
				}
			}
		}
		return changeNumber;
	}
	
	/***
	 * Check if the GR Object has pending change or not
	 * @param itemObject
	 * @return
	 * @throws APIException
	 */
	private boolean checkGRHasPendingChange(IItem grObject) throws APIException{
		boolean hasPendingGRO = false;
		if(grObject != null && !grObject.equals("")){
			hasPendingGRO = grObject.isFlagSet(ItemConstants.FLAG_HAS_PENDING_ECO);
			log.info("GR has pending change ? : "+hasPendingGRO);
		}	 
		return hasPendingGRO;
	}	
	
	/***
     * Method to read key/value pair from Property file
     * 
     * @param input
     * @return
     */
    public Object getBaseIdOrAPIName(final String input)
    {
        Object output = null;
        final String regex = "[0-9]+";
        if (input.matches(regex))
        {
            output = Integer.parseInt(input);
        }
        else
        {
            output = input;
        }
        return output;
    }

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}
	
	   
    
}
