package com.pepsico.agile.px;

import java.text.MessageFormat;
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
import com.agile.api.ITwoWayIterator;
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.px.util.PropertiesLoader;

public class GROChangeStatusFinalArttoReleasedPrePXEvent implements IEventAction{
    private static final String SUBCLASS_GRAPHIC_REQUISTION = "Graphic Requisition";
    private static final Logger LOGGER = Logger.getLogger(GROChangeStatusFinalArttoReleasedPrePXEvent.class.getName());
    @Override
    public EventActionResult doAction(IAgileSession session, INode arg1,
                                      IEventInfo eventInfo) {
        // Load Property file
        PropertiesLoader.loadResource("CornerStone");
        DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
        LOGGER.info("Inside GROChangeStatusFinalArttoReleasedPrePX");
        ActionResult result = null;
        IWFChangeStatusEventInfo wfStatusInfo = (IWFChangeStatusEventInfo) eventInfo;
        IChange groObject = null;
        try {
            groObject = (IChange) wfStatusInfo.getDataObject();
            LOGGER.info("Inside GROChangeStatusFinalArttoReleasedPrePX");
            LOGGER.info("GRO :"+groObject.getName());
            String brandMarkErrorMessage = checkBrandMarkStatus(groObject, session);
            if("".equalsIgnoreCase(brandMarkErrorMessage)){
                result = new ActionResult(ActionResult.STRING, "Success"); 
            }else{
                result = new ActionResult(ActionResult.EXCEPTION, new Exception(brandMarkErrorMessage));
            }

        } catch (APIException e) {
            LOGGER.error("API Exception - ",e);
            result = new ActionResult(ActionResult.EXCEPTION, e);
        } catch (Exception ex) {
            LOGGER.error("Exception - ",ex);
            result = new ActionResult(ActionResult.EXCEPTION, ex);
        }
        LOGGER.info("END of GROChangeStatusFinalArttoReleasedPrePX");
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
