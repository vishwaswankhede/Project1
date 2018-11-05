package com.pepsico.agile.events.bo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileList;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.IUserGroup;
import com.agile.api.ItemConstants;
import com.agile.api.UserGroupConstants;
import com.pepsico.agile.px.util.PropertiesLoader;

public class GROUpdateFunctionalTeamFromGRAffectedItemsPostPXBO {
private Logger logger=Logger.getLogger(GROUpdateFunctionalTeamFromGRAffectedItemsPostPXBO.class);
/**
 * This method return unique functional teams list from all the affected ITems from GRO
 * @param session
 * @param graphicRequisitionCO
 * @return
 * @throws APIException
 */
public Set<String> getFunctionalTeamsFromAffectedItemsGR(IAgileSession session, IChange graphicRequisitionCO) throws APIException
{
	logger.info("Inside getFunctionalTeamsFromAffectedItemsGR");
	Set<String> totalfunctionalTeams=new LinkedHashSet<String>();
	ITable affectedItemsTable=graphicRequisitionCO.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
	Iterator<IRow> affectedItemsItr=affectedItemsTable.getTableIterator();
	while(affectedItemsItr.hasNext())
	{
		IRow affectedItemRow=affectedItemsItr.next();
		String affectedItemType=affectedItemRow.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString();
		if(affectedItemType.equalsIgnoreCase(PropertiesLoader.getProperty("SUBCLASS_GRAPHIC_REQUISITION")))
		{
			String affectedItemsfunctionalTeams="";
			affectedItemsfunctionalTeams=affectedItemRow.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_FUNCTIONALTEAM).toString();
			logger.info("affectedItemsfunctionalTeams Length: "+affectedItemsfunctionalTeams.length());
			if(affectedItemsfunctionalTeams!=null && !("".equalsIgnoreCase(affectedItemsfunctionalTeams)))
			{
				String[] eachAffectedItemFunctionalTeam=affectedItemsfunctionalTeams.split(";");
				for(int i=0;i<eachAffectedItemFunctionalTeam.length;i++)
				{
					totalfunctionalTeams.add(eachAffectedItemFunctionalTeam[i]);
				}
				
			}
			else
			{
				totalfunctionalTeams.clear();
			}
		}
	}
	return totalfunctionalTeams;
	
}

/**
 * THis method sets the functional Teams in the Graphic Requisition CO cover page
 * @param session
 * @param finalFunctionalTeams
 * @param graphicRequisitionCO
 * @throws APIException 
 */
public void setFunctionalTeamsinGraphicRequisitionCO(IAgileSession session,Set<String> finalFunctionalTeams,IChange graphicRequisitionCO) throws APIException
{
	logger.info("Inside setFunctionalTeamsinGraphicRequisitionCO");
	ICell groFunctionalTeamCell=graphicRequisitionCO.getCell(PropertiesLoader.getProperty("FUNCTIONAL_TEAM"));
	List<Object> arrayList=new ArrayList<Object>();
	logger.info("groFunctionalTeamCell.getValue():"+groFunctionalTeamCell.getValue());
	if((groFunctionalTeamCell.getValue())==null)
	{
		 logger.info("gro functional team is null");
	}
	else
	{
		String functionalTeamsGRO=groFunctionalTeamCell.getValue().toString();
		String[] functionalTeamsGROList=functionalTeamsGRO.split(";");
		for(int i=0;i<functionalTeamsGROList.length;i++)
		{
			arrayList.add(session.getObject(UserGroupConstants.CLASS_FUNCTIONAL_TEAM, functionalTeamsGROList[i]));
		}
		logger.info("arrayList GRO:"+arrayList);
		
	}
	
	Iterator<String> functionalTeamsItr=finalFunctionalTeams.iterator();

	while(functionalTeamsItr.hasNext())
	{
		arrayList.add(session.getObject(UserGroupConstants.CLASS_FUNCTIONAL_TEAM, functionalTeamsItr.next()));
		logger.info("arrayList GR:"+arrayList);
	}
	arrayList.remove("");
	arrayList.remove(null);
	Object[] functionalTeams=new Object[arrayList.size()];
	arrayList.toArray(functionalTeams);
	
	// getting the existing value from GRCo functional Team
	
	
	IAgileList groFunctionalTeamList=groFunctionalTeamCell.getAvailableValues();
	groFunctionalTeamList.setSelection(functionalTeams);
	groFunctionalTeamCell.setValue(groFunctionalTeamList);
}
}
