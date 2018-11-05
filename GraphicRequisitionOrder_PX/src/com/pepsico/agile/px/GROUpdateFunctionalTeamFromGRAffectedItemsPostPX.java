package com.pepsico.agile.px;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.pepsico.agile.events.bo.GROUpdateFunctionalTeamFromGRAffectedItemsPostPXBO;
import com.pepsico.agile.px.util.PropertiesLoader;

public class GROUpdateFunctionalTeamFromGRAffectedItemsPostPX implements ICustomAction{

	private Logger logger=Logger.getLogger(GROUpdateFunctionalTeamFromGRAffectedItemsPostPX.class.getName());
	@Override
	public ActionResult doAction(final IAgileSession session, INode node, final IDataObject dataObject) {
	String message="";
		try {
			PropertiesLoader.loadResource("CornerStone");
			DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
			logger.info("executing GROUpdateFunctionalTeamFromGRCOPostPX ");
			final IChange graphicRequisitionCO=(IChange) dataObject;
			GROUpdateFunctionalTeamFromGRAffectedItemsPostPXBO grUpdateFunctionalTeam=new GROUpdateFunctionalTeamFromGRAffectedItemsPostPXBO();
			session.disableAllWarnings();
			
			Set<String> finalFunctionalTeams=grUpdateFunctionalTeam.getFunctionalTeamsFromAffectedItemsGR(session, graphicRequisitionCO);
			logger.info("finalFunctionalTeams:"+finalFunctionalTeams.size());
			Iterator itr=finalFunctionalTeams.iterator();
			if(!finalFunctionalTeams.isEmpty())
			{
				grUpdateFunctionalTeam.setFunctionalTeamsinGraphicRequisitionCO(session, finalFunctionalTeams, graphicRequisitionCO);
				message="Functional Team is updated successfully";
			}
			else
			{
				logger.info("Functional Team is not present in any of the GR");
				message="Functional Team is not present in any of the GR";
			}
			session.enableAllWarnings();
		} catch (APIException e) {
			logger.error("",e);
		}
		return new ActionResult(ActionResult.STRING,message);
	}

}
