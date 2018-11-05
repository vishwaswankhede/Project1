/**
 * 
 */
package com.pepsico.agile.events;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.AgileSessionFactory;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.events.bo.GROChangeStatusApprovalToPrePressBO;
import com.pepsico.agile.px.common.PasswordUtil;
import com.pepsico.agile.px.util.PropertiesLoader;

/**
 * Change Status event on GR from Approval to Pre-press
 * To update PVSpec on TradeGTIN from GR
 * @author 09163426
 *
 */
public class GROChangeStatusApprovalToPrePressEventPX implements IEventAction{
	private Logger log = Logger.getLogger(GROChangeStatusApprovalToPrePressEventPX.class.getName());
	private  static String userName = "";
    private  static String password = "";
    private  static String agileURL = "";

	@Override
	public EventActionResult doAction(IAgileSession agileSession, INode node, IEventInfo eventInfo) {
		ActionResult actionResult = null;
		try{
			String eventBannerMessage="";
			PropertiesLoader.loadResource("CornerStone");
			DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
			 //Creating Pxadmin session to log the history on Preliminary TGTIN : Defect 2708
			initialize();
			agileSession = login();
			log.info("****** START OF GRO change status event - GROChangeStatusApprovalToPrePress ****");
			final IWFChangeStatusEventInfo changeStatusEvent = (IWFChangeStatusEventInfo) eventInfo;
	        final IChange graphicRequisitionChangeOrder = (IChange) changeStatusEvent.getDataObject();
	        final GROChangeStatusApprovalToPrePressBO bo = new GROChangeStatusApprovalToPrePressBO();
	        eventBannerMessage  = bo.getGROAffectedItem(graphicRequisitionChangeOrder, agileSession);
	      
	        
	        if (!"".equalsIgnoreCase(eventBannerMessage) && bo.flag){
                actionResult = new ActionResult(ActionResult.STRING, eventBannerMessage);
            }
	        else if(!"".equalsIgnoreCase(eventBannerMessage) && !bo.flag){
	        	actionResult = new ActionResult(ActionResult.STRING, eventBannerMessage);
	        }else{
                actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(eventBannerMessage));
            }
            log.info("------ End of the Change Status Event - GROChangeStatusApprovalToPrePress ----------");
		}catch (APIException api)
		{
			log.error("API Error : ", api); 
			actionResult = new ActionResult(ActionResult.EXCEPTION, api);	         
		}
		catch (Exception ex)
		{
            log.error("Error : ", ex);
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
		}
		return new EventActionResult(eventInfo, actionResult);
	}
	
	private static void initialize() throws APIException {

		String strAgileURL = PropertiesLoader.getProperty("GTINRequestCredential");
		String credentials[] = strAgileURL.split(":");
		PasswordUtil util = new PasswordUtil();

		// printMessage(credentials[0]+"://"+credentials[1]+":"+credentials[2]+"/Agile");
		userName = credentials[3];
		password = util.decryptPWD(credentials[4]);
		agileURL = credentials[0] + "://" + credentials[1] + ":" + credentials[2] + "/Agile";
		//session = login();

	}

	private IAgileSession login() throws APIException {

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
		log.info("PxAdmin Session is created" + session.getCurrentUser());

		return session;
	}

}
