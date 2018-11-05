package com.pepsico.agile.px;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.AgileSessionFactory;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileClass;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.IUser;
import com.agile.api.ItemConstants;
import com.agile.api.StatusConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.px.common.PXConstants;
import com.pepsico.agile.px.common.PasswordUtil;
import com.pepsico.agile.px.util.PropertiesLoader;

public class GROChangeStatusApprovalstoVendorPrePXEvent implements IEventAction{
	private  static String userName = "";
    private  static String password = "";
    private  static String agileURL = "";
    private static final String SUBCLASS_GRAPHIC_REQUISTION = "Graphic Requisition";
    private static final Logger LOGGER = Logger.getLogger(GROChangeStatusApprovalstoVendorPrePXEvent.class.getName());
    @Override
    public EventActionResult doAction(IAgileSession session, INode arg1,
                                      IEventInfo eventInfo) {
        // Load Property file
        PropertiesLoader.loadResource("CornerStone");
        DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
        LOGGER.info("Inside GROChangeStatusApprovalstoVendorPrePX");
        ActionResult result = null;
        try {
        	 //Creating Pxadmin session to log the history on Preliminary TGTIN : Defect 2708
        initialize();
        session = login();
        IWFChangeStatusEventInfo wfStatusInfo = (IWFChangeStatusEventInfo) eventInfo;
        IChange groObject = null;
       
            groObject = (IChange) wfStatusInfo.getDataObject();
            LOGGER.info("Inside GROChangeStatusApprovalstoVendorPrePX");
            LOGGER.info("GRO :"+groObject.getName());
            String errorMessage = populateTradeGtinDimensions(groObject,session);
            String brandMarkErrorMessage = checkBrandMarkStatus(groObject, session);
            errorMessage = errorMessage + brandMarkErrorMessage;
            if("".equalsIgnoreCase(errorMessage)){
                result = new ActionResult(ActionResult.STRING, "Success"); 
            }else{
                result = new ActionResult(ActionResult.EXCEPTION, new Exception(errorMessage));
            }

        } catch (APIException e) {
            LOGGER.error("API Exception - ",e);
            result = new ActionResult(ActionResult.EXCEPTION, e);
        } catch (Exception ex) {
            LOGGER.error("Exception - ",ex);
            result = new ActionResult(ActionResult.EXCEPTION, ex);
        }
        LOGGER.info("END of GROChangeStatusApprovalstoVendorPrePX");
        return new EventActionResult(eventInfo, result);
    }
    /**
     * Append the messaged for each GR and display it in the banner
     * 
     * @param message
     */
    private void setBannerMessage(final StringBuilder bannerMessage, final String message)
    {
        bannerMessage.append(message).append("\n");
    }
    
    /**
     * 
     * @param groObject
     * @param session
     * @return
     * @throws APIException
     */
    private String populateTradeGtinDimensions(IChange groObject, IAgileSession session) throws APIException
    {  
    	String WORKFLOW_NAMES_IGNORE_PV_SPEC_VALIDATION=PropertiesLoader.getProperty("WORKFLOW_NAMES_IGNORE_PV_SPEC_VALIDATION");
    	boolean isWorkflowValidationAllowed=true;
    	String errorMessage = "";
        final StringBuilder bannerMessage = new StringBuilder();
        ITable affectedItemsTable = groObject
            .getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
        //Start
        String workflowName=groObject.getWorkflow().getName();
        String[] ignoreValidationWf=WORKFLOW_NAMES_IGNORE_PV_SPEC_VALIDATION.split(";");
        for(int i=0;i<ignoreValidationWf.length;i++)
        {
        	if(ignoreValidationWf[0].equalsIgnoreCase(workflowName)) 
        	{
        		isWorkflowValidationAllowed=false;
        		break;
        	}
        }
        //End
        ITwoWayIterator iterator = affectedItemsTable.getTableIterator();
        LOGGER.info("Inside function populateTradeGtinDimensions");
        while (iterator.hasNext()) {    
            LOGGER.info("Iterating through GRO");
            IRow childRow = (IRow) iterator.next();
            IItem item = (IItem) childRow.getReferent();
            String itemType = item.getValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE).toString();
            LOGGER.info("Item Type :"+itemType);
            if(itemType.equalsIgnoreCase(SUBCLASS_GRAPHIC_REQUISTION))
            {
                ITable redlinePageThreeTable = item.getTable(ItemConstants.TABLE_REDLINEPAGETHREE);
                Iterator<IRow> itr = redlinePageThreeTable.getTableIterator();
                IRow redlineItem = itr.next();
                if(redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST18)==null||redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST18).toString().isEmpty()){
                    errorMessage = "Please update the Item Reference Number with a Trade GTIN to "+itemType+" "+item.getName();
                    setBannerMessage(bannerMessage, errorMessage);
                }
                if(isWorkflowValidationAllowed && (redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST02)==null||redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST02).toString().isEmpty())){
                		errorMessage = "Please update the Packaging Specification ID with a PV Spec ID for the "+itemType+" "+item.getName();
                        setBannerMessage(bannerMessage, errorMessage);
                }else{
                    String packagingSpecIDNumber = redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST02).toString();                
                    IItem packagingSpecID = (IItem) session.getObject(IItem.OBJECT_TYPE, packagingSpecIDNumber);
                    LOGGER.info("packagingSpecIDNumber "+packagingSpecIDNumber);
                    String tradeGTINNumber = redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST18).toString();              
                    LOGGER.info("tradeGTINNumber "+tradeGTINNumber);
                    IItem tradeGTIN = (IItem) session.getObject(IItem.OBJECT_TYPE, tradeGTINNumber);
                    //checking if Trade GTIN has any pending changes
                    ITable tradeGTINPendingChange=tradeGTIN.getTable(ItemConstants.TABLE_PENDINGCHANGES);
                    LOGGER.info("tradeGTIN pending change table size:"+tradeGTINPendingChange.size());
                    //WIT 1542 - changes starts
//                    if(!"Bag Sizing/Flex".equalsIgnoreCase(packagingSpecID.getValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE).toString())){
                    if(!"Trade GTIN".equalsIgnoreCase(tradeGTIN.getValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE).toString())){
                        errorMessage = "The Item reference number object is not a Trade GTIN. Please add a Trade GTIN object";
                        setBannerMessage(bannerMessage, errorMessage);

                    }
                    if(isWorkflowValidationAllowed)
                    {
                    	if(!PXConstants.pvResources.contains(packagingSpecID.getValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE).toString())){
                            errorMessage = "The Packaging Specification ID object selected on the Graphic Requisition is not a PV Resource. Please select a PV Resource object";
                            setBannerMessage(bannerMessage, errorMessage);

                        }
                    	else if((packagingSpecID.getValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE).toString()).equalsIgnoreCase("Bag Sizing/Flex") && isWorkflowValidationAllowed){//added if condition as part of WIT 1542
                        LOGGER.info("Setting values");
                        Object width = ""; 
                        width = packagingSpecID.getValue(ItemConstants.ATT_PAGE_THREE_NUMERIC01);
                        Object length = "";
                        length = packagingSpecID.getValue(ItemConstants.ATT_PAGE_THREE_NUMERIC02);
                        //Including below attributes for defect 2709
                        Object layDownWidth ="";
                        layDownWidth =packagingSpecID.getValue(ItemConstants.ATT_PAGE_THREE_NUMERIC10);
                        ICell height = packagingSpecID.getCell("heightInches");
                        Object layDownHeight="";
                        layDownHeight=height.getValue();
                        Object layDownDepth ="";
                        layDownDepth=packagingSpecID.getValue(ItemConstants.ATT_PAGE_THREE_NUMERIC09);
                        Object footPrintWidth ="";
                        //double result = 0  ;
                        double formerBagWidth = Double.parseDouble(width.toString());
                        double lengthRepeat = Double.parseDouble(length.toString());
    	           		if(formerBagWidth>=7){
    	           			if(lengthRepeat/formerBagWidth>=1.6){
    	           			 footPrintWidth  =( formerBagWidth - 0.5)/2.54 ;
    	           			
    	           		}else if(lengthRepeat/formerBagWidth<1.6){
    	           			 footPrintWidth  =( formerBagWidth - 1)/2.54 ;
    	           		}
    	           		}else if(formerBagWidth<7){
    	           			footPrintWidth =  formerBagWidth / 2.54 ;	
    	           		}
                        Map<Object,Object> params = new HashMap<Object,Object>();
                        if (width != null ) {
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC08, width);
                        } else{
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC08, "0");
                        }
                        if (length != null ){
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC07, length);
                        }else{
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC07, "0");
                        }
                        if (layDownWidth != null ){
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC04, layDownWidth);
                        }else{
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC04, "0");
                        }
                        if (layDownHeight != null ){
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC02, layDownHeight);
                        }else{
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC02, "0");
                        }
                        if (layDownDepth != null ){
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC03, layDownDepth);
                        }else{
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC03, "0");
                        }
                        if (footPrintWidth != null ){
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC09, footPrintWidth);
                        }else{
                            params.put(ItemConstants.ATT_PAGE_THREE_NUMERIC09, "0");
                        }
                        
                        setDimensionValuesOnObject(session, tradeGTIN, params,groObject,(IItem) redlineItem.getReferent());
                    }
                    }
                }
            }else{
                LOGGER.info("Affected Item is not GR. So skip");
            }

        }
        return bannerMessage.toString();
    }

    /**
     * Set dimension data on GTIN from PV Specification
     * @param session
     * @param tradeGTIN
     * @param params
     * @throws APIException
     */
    private void setDimensionValuesOnObject(final IAgileSession session,final IItem tradeGTIN,final Map<Object,Object> params,IChange groObject,IItem grObject) throws APIException{

        final ITable pendingChanges = tradeGTIN.getTable(ItemConstants.TABLE_PENDINGCHANGES);
       // List<IUser> list = new ArrayList<IUser>();
       // list.add(session.getCurrentUser());
        String PVNum = tradeGTIN.getValue(ItemConstants.ATT_PAGE_THREE_LIST09).toString();
        if("Preliminary".equalsIgnoreCase(tradeGTIN.getValue(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE).toString())&& pendingChanges.isEmpty() && (PVNum.equals("")||PVNum==null)){
            //Set values directly
            tradeGTIN.setValues(params);
            tradeGTIN.logAction("Dimension attributes have been updated on TGTIN "+tradeGTIN+" associated to the GR "+grObject+" and GRCO " +groObject);
     
        }
        //Commented else loop to restrict TGTIN update on pending CO or new CO  : Defect Number:2708
        /*else if(!pendingChanges.isEmpty()){
            //Set values as redline for the pending changes
            session.disableAllWarnings();
            final Iterator<IRow> pendingIter = pendingChanges.iterator();
            while(pendingIter.hasNext()){
                IRow changesRow = pendingIter.next();
                tradeGTIN.setRevision(changesRow.getReferent().getName());
                updateAttributes(tradeGTIN, params);
            }
            session.sendNotification(tradeGTIN, PropertiesLoader.getProperty("GTIN_AUTO_UPDATE_CHG_NOTIFICATION"), list, false,"");
            session.enableAllWarnings();
        }
        else{
            //Create change and modify values
            session.disableAllWarnings();
            final IChange changeObj = createChangeToReleaseGTIN(session, tradeGTIN,params);
            changeObj.refresh();
            //release change
            releaseChange(changeObj);
            session.sendNotification(tradeGTIN, PropertiesLoader.getProperty("GTIN_AUTO_UPDATE_CHG_NOTIFICATION"), list, false,"");
            session.enableAllWarnings();
        }*/
    }
    
    private static  void initialize() throws APIException
    {
            String strAgileURL = PropertiesLoader.getProperty("GTINRequestCredential");
            String credentials[] = strAgileURL.split(":");
            PasswordUtil util = new PasswordUtil();

           LOGGER.info(credentials[0]+"://"+credentials[1]+":"+credentials[2]+"/Agile");
            userName = credentials[3];
            password = util.decryptPWD(credentials[4]);
            agileURL = credentials[0] + "://" + credentials[1] + ":" + credentials[2] + "/Agile";
          
        }
      
	private  static IAgileSession login() throws APIException {

		String USERNAME = userName;
		String PASSWORD = password;
		String URL = agileURL;

		IAgileSession session = null;
		AgileSessionFactory factory = null;
		factory = AgileSessionFactory.getInstance(URL);
		HashMap params = new HashMap();
		params.put(AgileSessionFactory.USERNAME, USERNAME);
		params.put(AgileSessionFactory.PASSWORD, PASSWORD);
		session = factory.createSession(params);
		LOGGER.info("PxAdmin Session is created" + session.getCurrentUser());

		return session;
	}

    /**
     * Create change to make GTIN updates and release it.
     * @param session
     * @param tradeGTIN
     * @param valuesToBeSet
     * @return
     * @throws APIException
     */


    private IChange createChangeToReleaseGTIN(IAgileSession session ,final IItem tradeGTIN,final Map<Object,Object> valuesToBeSet) throws APIException{

        final Map<Integer, Object> params = new HashMap<Integer, Object>();
        params.put(ChangeConstants.ATT_COVER_PAGE_DESCRIPTION, PropertiesLoader.getProperty("GTIN_AUTO_UPDATE_CHG_DESCRIPTION"));
        params.put(ChangeConstants.ATT_COVER_PAGE_WORKFLOW, PropertiesLoader.getProperty("GTIN_AUTO_UPDATE_CHG_WF"));
        final IAgileClass fgcoClass = session.getAdminInstance().getAgileClass(PropertiesLoader.getProperty("GTIN_AUTO_UPDATE_CHG_AGILECLASS"));
        final String nextAutoNumber = fgcoClass.getAutoNumberSources()[0].getNextNumber();
        final IChange change = (IChange)session.createObject(fgcoClass,nextAutoNumber);
        change.setValues(params);
        //Add TradeGTIN to change and update the new LCP
        final IRow gtinRow = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS).createRow(tradeGTIN);
        final Object oldLCP = gtinRow.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_LIFECYCLE_PHASE);
        final Object newLCP = gtinRow.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE);
        if(newLCP == null || "".equalsIgnoreCase(newLCP.toString())){
            gtinRow.setValue(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE, oldLCP);
        }
        tradeGTIN.setRevision(change.getName());
        updateAttributes(tradeGTIN, valuesToBeSet);
        return change;
    }

    /**
     * Update attributes on GTIN
     * @param itemObj
     * @param valuesToBeSet
     * @throws APIException
     */
    private void updateAttributes(final IItem itemObj,final Map<Object,Object> valuesToBeSet) throws APIException{
        final ITable table = itemObj.getTable(ItemConstants.TABLE_REDLINEPAGETHREE);
        final IRow redlineRow = (IRow)table.iterator().next();
        redlineRow.setValues(valuesToBeSet);
    }
    /**
     * Method to release a change
     * @param change
     * @throws APIException
     */
    private void releaseChange(final IChange change) throws APIException{
        
        if(!(change.getStatus().getStatusType() == StatusConstants.TYPE_COMPLETE)){
            change.changeStatus(change.getDefaultNextStatus(), true, "", true, true,null, null, null, null, false);
        }
    }
    
    /**
     * Method to check Brandmark lifecycle status on GR
     * */
     public String checkBrandMarkStatus(IChange groObject,IAgileSession session) throws APIException
     {
    	 String message = "";
    	 final StringBuilder bannerMessage = new StringBuilder();
    	 ITable affectedItemsTable = groObject
    	            .getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
    	 ITwoWayIterator iterator = affectedItemsTable.getTableIterator();
         while (iterator.hasNext()) {    
             LOGGER.info("Iterating through GRO");
             IRow childRow = (IRow) iterator.next();
             IItem item = (IItem) childRow.getReferent();
             String itemType = item.getValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE).toString();
             LOGGER.info("Item Type :"+itemType);
             if(itemType.equalsIgnoreCase(SUBCLASS_GRAPHIC_REQUISTION))
             {
            	 ITable redlinePageThreeTable = item.getTable(ItemConstants.TABLE_REDLINEPAGETHREE);
                 Iterator<IRow> itr = redlinePageThreeTable.getTableIterator();
                 IRow redlineItem = itr.next();
                 if(redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST03)!=null && 
                		 !redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST03).toString().equals(""))
                 {
                	 IItem brandMark = (IItem)session.getObject(IItem.OBJECT_TYPE, redlineItem.getValue(ItemConstants.ATT_PAGE_THREE_LIST03).toString());
                	 if(brandMark.getValue(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE).toString()
                			 .equals(PropertiesLoader.getProperty("LIFECYCLE_PHASE_PRELIMINARY")))
                	 {
                		 message = MessageFormat.format(PropertiesLoader.getProperty("INVALID_BRANDMARK_STATUS"), new Object[]{item});
                		 setBannerMessage(bannerMessage, message);
                	 }
                 }
                 
             }
         }
    	 return bannerMessage.toString();
     }

}
