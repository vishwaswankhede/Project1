/**
 * 
 */
package com.pepsico.agile.events;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IManufacturer;
import com.agile.api.INode;
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.events.bo.GROChangeStatusMarketingServicesToApprovalsBO;
import com.pepsico.agile.events.bo.GROUPCMappingItemRefNumber;
import com.pepsico.agile.px.util.PropertiesLoader;

/**
 * @author 09165508 
 * Change Status Pre event which is used to validate if the GTIN selected on the GR
 *         is correct. If not user wont be allowed to move to the next status.
 */
/**
 * 
 * @author 09158711 Validating if Functional Team in GRCO is equal to Functional
 *         Teams in All GR from Affected items of GRCO. If not correct, user is
 *         forced to run the Action PX.
 */
public class GROChangeStatusMarketingServicesToApprovals implements
		IEventAction {
	private static final Logger LOGGER = Logger
			.getLogger(GROChangeStatusMarketingServicesToApprovals.class
					.getName());

	@Override
	public EventActionResult doAction(IAgileSession agileSession, INode node,
			IEventInfo eventInfo) {
		ActionResult actionResult = null;
		try {
			// Load Property file
			PropertiesLoader.loadResource("CornerStone");
			DOMConfigurator.configure(PropertiesLoader
					.getProperty("LOG4J_CONFIG"));
			LOGGER.info("Start of the Change Status Event - GROChangeStatusMarketingServicesToApprovals");
			final IWFChangeStatusEventInfo changeStatusEvent = (IWFChangeStatusEventInfo) eventInfo;
			final IChange graphicRequisitionOrder = (IChange) changeStatusEvent
					.getDataObject();
			final GROChangeStatusMarketingServicesToApprovalsBO bo = new GROChangeStatusMarketingServicesToApprovalsBO(
					agileSession, graphicRequisitionOrder, LOGGER);
			StringBuilder bannerMesage = new StringBuilder();
			bannerMesage.append(bo
					.validateGraphicRequisition(graphicRequisitionOrder));
			bannerMesage.append(bo.getFunctionalTeamsfromGRCO());
			/**
			 * @author 09176239
			 * calling the method for the UPC auto population 
			 * UPC from the redlined Trade GTIN is set to the UPC of GR
			 * while moving the change status from the marketing services to the approval status  
			 */
			
			
			GROUPCMappingItemRefNumber upcMap = new   GROUPCMappingItemRefNumber();
			upcMap.UpdateTitleBlockUPC(agileSession, graphicRequisitionOrder);
			
			//if("".equalsIgnoreCase(bannerMesage.toString()))
			if (bannerMesage.toString().isEmpty()) {
				actionResult = new ActionResult(ActionResult.STRING,
						"Validation Successful.");
			} else {
				actionResult = new ActionResult(ActionResult.EXCEPTION,
						new Exception(bannerMesage.toString()));
			}
			LOGGER.info("End of the Change Status Event - GROChangeStatusMarketingServicesToApprovals");

		} catch (APIException api) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, api);
			LOGGER.error("API Error : ", api);
			// Need to handle exception.
		} catch (Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
			LOGGER.error("Error : ", ex);
		}
		return new EventActionResult(eventInfo, actionResult);
	}

}
