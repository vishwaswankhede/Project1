package com.pepsico.agile.events;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;

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
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.px.util.PropertiesLoader;

public class ValidateMandatoryAttributesFromPrePressStagingToPrePressIntegration implements IEventAction
{

    private static final Logger LOGGER = Logger.getLogger(ValidateMandatoryAttributesFromPrePressStagingToPrePressIntegration.class.getName());


    public EventActionResult doAction(IAgileSession agileSession,
                                      INode node,
                                      IEventInfo eventInfo)
    {
        ActionResult actionResult = null;
        try
        {

            PropertiesLoader.loadResource("CornerStone");
            DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
            actionResult = new ActionResult(ActionResult.STRING, Constants.SUCCESS_MESSAGE);
            LOGGER.info("Start validation to check if Final Artwork Approval is  required.");
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
                builder.append(validateIfFinalApprovedArtReceivedDateIsRequired(grObject,grco));
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


    /**
     * Validate if 'Final Approved Artwork Received Date' is mandatory
     * 
     * @param grObject
     * @return
     * @throws APIException
     */
    private String validateIfFinalApprovedArtReceivedDateIsRequired(final IItem grObject,final IChange grco)
        throws APIException
    {
        String returnMessage = "";
        final String designArtworkRequired = getAttributeValueFromObject(grObject, grco, Constants.APINAME_DESIGNARTWORKREQUIRED);
        final String finalArtworkRecievedDate = getAttributeValueFromObject(grObject, grco, Constants.BASEID_FINALARTWORKRECIEVEDDATE);

        LOGGER.info("designArtworkRequired asscoiated with GR = " + designArtworkRequired);
        if ((designArtworkRequired.equalsIgnoreCase(Constants.YES_EXISTING) || designArtworkRequired.equalsIgnoreCase(Constants.YES_NEW))&&
            finalArtworkRecievedDate.isEmpty())
        {
            returnMessage = MessageFormat.format(Constants.MSG_MADATORY_FINALARTWORKRECIEVEDDATE,
                                                 new Object[] {
                                                     grObject.getName()
                                                 });
            LOGGER.info("Final Artwork Approval Date is mandatory");

        }
        return returnMessage;
    }
    
    /**
     * Get redlined or attribute value from the object
     * @param grObject
     * @param grco
     * @param attributeName
     * @return
     * @throws APIException
     */
    private String getAttributeValueFromObject(final IItem grObject,final IChange grco,final Object attributeName) throws APIException
    {
        String returnValue = "";
        grObject.setRevision(grco);
        final Iterator<IRow> p3Attrs = grObject.getTable(ItemConstants.TABLE_REDLINEPAGETHREE).iterator();
        final IRow p3Row = p3Attrs.next();
        final Object object = grObject.getCell(attributeName).getId();
        final Set<Object> keyset = p3Row.getValues().keySet();
        if (keyset.contains(attributeName) || keyset.contains(object))
        {
            final Object obj = p3Row.getValue(attributeName);
            if (obj != null)
            {
                returnValue = obj.toString();
            }
        }
        else
        {
            if (grObject.getValue(attributeName) != null)
            {
                returnValue = grObject.getValue(attributeName).toString();
            }
        }
        return returnValue;
    }

}
