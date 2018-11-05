package com.pepsico.agile.events;

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

public class ValidateBOMFromApprovalsToPrepress implements IEventAction{

	private static final Logger LOGGER = Logger.getLogger(ValidateBOMFromApprovalsToPrepress.class.getName());
	private StringBuilder finalMessage= new StringBuilder();
    public EventActionResult doAction(IAgileSession agileSession,
                                      INode node,
                                      IEventInfo eventInfo)
    {
        ActionResult actionResult = null;
        try
        {

            PropertiesLoader.loadResource("CornerStone");
            DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
            actionResult = new ActionResult(ActionResult.STRING, Constants.MSG_SUCCESS_BOM_VALIDATION);
            LOGGER.info("Start validation Approval to next status.");
            final StringBuilder builder = new StringBuilder();
            final IWFChangeStatusEventInfo changeStatusEvent = (IWFChangeStatusEventInfo) eventInfo;
            final IChange grco = (IChange) changeStatusEvent.getDataObject();
            final ITable aiTable = grco.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
            final Iterator<IRow> iterator = aiTable.iterator();
            while (iterator.hasNext())
            {
                final IRow row = iterator.next();
                final IItem grObject = (IItem) row.getReferent();
                LOGGER.info("GR to be validated = " + grObject);                
                builder.append(validateBOMData(grObject,grco));                
            }

            final String errorMsg = builder.toString();
            if (!errorMsg.isEmpty())
            {
                actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(errorMsg));
            }
            LOGGER.info("End validation to check if Final Artwork Approval is  required.");

        }
        catch (Exception ex)
        {
            LOGGER.error("", ex);
            actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(Constants.FAILURE_MESSAGE));
        }
        return new EventActionResult(eventInfo, actionResult);
    }
    private String validateBOMData(final IItem grObject,final IChange grco)
            throws APIException
        {
            String returnMessage = "";
            final String designArtworkRequired = "";
            final String finalArtworkRecievedDate = "";
            IItem item=null;
            boolean condition1 = false;
            boolean condition2 = true;
            ITable bomTable = grObject.getTable(ItemConstants.TABLE_REDLINEBOM);
            ITwoWayIterator itr = bomTable.getTableIterator();
            while(itr.hasNext())
            {
            	IRow iRow = (IRow) itr.next();
    			item = (IItem) iRow.getReferent();
    			LOGGER.info("Item type :"+item.getValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE));
    			if(iRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED)==false)
    			{
    			if(item.getValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE).toString().equals(Constants.SUBCLASS_LABEL_SPECIFICATION))
    			{	
    				condition1 = true;
    				String statementStatus = iRow.getValue(ItemConstants.ATT_BOM_BOM_LIST08).toString();    				
    				if(iRow.getValue(ItemConstants.ATT_BOM_BOM_NUMERIC01)==null||statementStatus.equals("")||statementStatus.isEmpty())
    				{
    					LOGGER.info("Inside Condition2 check");
    					condition2 = false; 
    				}
    				else
    				{
    					String count = iRow.getValue(ItemConstants.ATT_BOM_BOM_NUMERIC01).toString();
        				LOGGER.info("Count of Label Specification "+item.getName()+":"+count);
        				LOGGER.info("Statement Status of Label Specification "+item.getName()+":"+statementStatus);
    				}
    			}
    			}
            }
            LOGGER.info("Condition 1"+condition1);
            LOGGER.info("Condition 2"+condition2);
            if(condition1==false)
            {	returnMessage = MessageFormat.format(Constants.MSG_MANDATORY_BOM_MISSING,
                        new Object[] {
                            grObject.getName()
                        });
            }else if(condition1==true && condition2 == false)
            {
            	returnMessage = MessageFormat.format(Constants.MSG_MANDATORY_COUNT_STATEMENTSTATUS,
                        new Object[] {
                            grObject.getName()});        
            }
            else
            	LOGGER.info("BOM validated successfully");
            	
            LOGGER.info("End of BOM Data "+returnMessage);
            return returnMessage;
        }
    
}
