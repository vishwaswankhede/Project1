/**
 * 
 */
package com.pepsico.agile.events;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.events.bo.GROChangeStatusMarketingServicesToApprovalsBO;
import com.pepsico.agile.px.util.PropertiesLoader;

/**
 * 
 * @author 09158711
 * Validating if Functional Team in GRCO contains Functional Teams in All GR from Affected items of GRCO.
 *  If not correct, user is forced to run the Action PX.
 */
public class GROChangeStatusMarketingServicesToPurchasingAccountingPrePX implements IEventAction
{
    private static final Logger LOGGER = Logger.getLogger(GROChangeStatusMarketingServicesToPurchasingAccountingPrePX.class.getName());


    @Override
    public EventActionResult doAction(IAgileSession agileSession,
                                      INode node,
                                      IEventInfo eventInfo)
    {
        ActionResult actionResult = null;
        try
        {
            // Load Property file
            PropertiesLoader.loadResource("CornerStone");
            DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
            LOGGER.info("Start of the Change Status Event - GROChangeStatusMarketingServicesToApprovals");
            final IWFChangeStatusEventInfo changeStatusEvent = (IWFChangeStatusEventInfo) eventInfo;
            final IChange graphicRequisitionOrder = (IChange) changeStatusEvent.getDataObject();
            final GROChangeStatusMarketingServicesToApprovalsBO bo = new GROChangeStatusMarketingServicesToApprovalsBO(agileSession,
                                                                                                                       graphicRequisitionOrder,
                                                                                                                       LOGGER);
            String bannerMesage ="";
            bannerMesage= bo.validateGraphicRequisition(graphicRequisitionOrder);
            bannerMesage=bo.getFunctionalTeamsfromGRCO();
            if ("".equalsIgnoreCase(bannerMesage))
            {
                actionResult = new ActionResult(ActionResult.STRING, "Validation Successful.");
            }
            else
            {
                actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(bannerMesage));
            }
            LOGGER.info("End of the Change Status Event - GROChangeStatusMarketingServicesToApprovals");

        }
        catch (APIException api)
        {
            actionResult = new ActionResult(ActionResult.EXCEPTION, api);
            LOGGER.error("API Error : ", api);
            // Need to handle exception.
        }
        catch (Exception ex)
        {
            actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
            LOGGER.error("Error : ", ex);
        }
        return new EventActionResult(eventInfo, actionResult);
    }

}
