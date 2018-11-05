package com.pepsico.agile.events;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.px.util.PropertiesLoader;

public class UpdateMarketingCopyDescriptionFromBrandMarkDescription implements IEventAction
{

    private static final Logger LOGGER = Logger.getLogger(UpdateMarketingCopyDescriptionFromBrandMarkDescription.class.getName());

    @Override
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
            LOGGER.info("Start Marketing Copy Description Update .");
            final StringBuilder builder = new StringBuilder();
            final IWFChangeStatusEventInfo changeStatusEvent = (IWFChangeStatusEventInfo) eventInfo;
            final IChange grco = (IChange) changeStatusEvent.getDataObject();
            final ITable aiTable = grco.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
            final Iterator<IRow> iterator = aiTable.iterator();
            while (iterator.hasNext())
            {
                final IRow row = iterator.next();
                final IItem grObject = (IItem) row.getReferent();
                
            }

            
            LOGGER.info("Endtart Marketing Copy Description Update .");

        }
        catch (Exception ex)
        {
            LOGGER.error("", ex);
            actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(Constants.FAILURE_MESSAGE));
        }
        return new EventActionResult(eventInfo, actionResult);

    }
}
