/**
 * 
 */
package com.pepsico.agile.events.bo;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.IManufacturer;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ItemConstants;
import com.pepsico.agile.px.util.PropertiesLoader;

/**
 * @author 09165508 BO which is used to validate if the GTIN selected on the GR
 *         is correct. If not an error message will be displayed.
 */
public class GROChangeStatusMarketingServicesToApprovalsBO {
	private Logger logger = null;
	private IAgileSession agileSession = null;
	private StringBuilder bannerMessage = new StringBuilder();
	private IChange graphicRequisitionOrder = null;

	public GROChangeStatusMarketingServicesToApprovalsBO(final IAgileSession agileSession,
			final IChange graphicRequisitionOrder, final Logger logger) {
		this.logger = logger;
		this.agileSession = agileSession;
		this.graphicRequisitionOrder = graphicRequisitionOrder;
	}

	/**
	 * Main class called for validating the GR against the Product and GTIN
	 * 
	 * @param graphicRequisitionOrder
	 * @return
	 * @throws APIException
	 */
	
	public String validateGraphicRequisition(final IChange graphicRequisitionOrder) throws APIException {
		logger.info("Start validation for GRO - " + graphicRequisitionOrder.getName());
		final Iterator<IRow> grIter = getGRObjectsAssociatedWithGRO(graphicRequisitionOrder);
		           
		while (grIter.hasNext()) {
			final IRow affItemRow = grIter.next();
			final IItem grItem = (IItem) affItemRow.getReferent();
			final IItem fgProdItem = getProductAssociatedWithGR(grItem);
	        validateGraphicReqWithFGProduct(fgProdItem, grItem);
		}
		return bannerMessage.toString();
	}

	/**
	 * Get required attribute associated with GR. If GR has pending change then
	 * get it from inputed table table else get it directly
	 * 
	 * @param dataObj
	 * @param attrTableID
	 * @param changeObj
	 * @param attrName
	 * @param isChangeControlled
	 * @return
	 * @throws APIException
	 */
	@SuppressWarnings("unchecked")
	private String getAttributeValueFromDataObj(final IItem dataObj, final Integer attrTableID, final IChange changeObj,
			final String attrName, final boolean isChangeControlled) throws APIException {
		String attrValue = "";
		// If change controlled get from redline table
		dataObj.setRevision(changeObj);
		if (isChangeControlled) {
			final Iterator<IRow> p3Attrs = dataObj.getTable(attrTableID).iterator();
			IRow p3Row = p3Attrs.next();
			attrValue = p3Row.getValue(getBaseIdOrAPIName(attrName)).toString();
		} else {
			// Else get directly
			attrValue = dataObj.getValue(getBaseIdOrAPIName(attrName)).toString();
		}
		return attrValue;
	}

	/**
	 * Append the messaged for each GR and display it in the banner
	 * 
	 * @param message
	 */
	private void setBannerMessage(final String message) {
		bannerMessage.append(message).append("\n");
	}

	/**
	 * Get dataobject for the inputed string value
	 * 
	 * @param dataObjName
	 * @param objType
	 * @return
	 * @throws APIException
	 */
	private IDataObject getDataObjectFromString(final String dataObjName, final Integer objType) throws APIException {
		IDataObject dataObject = null;
		if (!"".equalsIgnoreCase(dataObjName)) {
			dataObject = (IItem) agileSession.getObject(objType, dataObjName.toString().trim());
		}
		return dataObject;
	}

	/**
	 * Validate the FG product items associated with GR. If there are no
	 * products or more than 1 product record it and if only one product found
	 * then return it
	 * 
	 * @param grItemObj
	 * @return
	 * @throws APIException
	 */
	private IItem getProductAssociatedWithGR(final IItem grItemObj) throws APIException {
		IItem fgProductItem = null;
		final ITable relTable = grItemObj.getTable(ItemConstants.TABLE_RELATIONSHIPS)
				.where(PropertiesLoader.getProperty("QUERY_GET_FGPRODUCT"), null);
		// If not products associated with GR
		if (relTable.isEmpty()) {
			setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("GR_WITH_NO_PRODUCT"),
					new Object[] { grItemObj.getName() }));

		} else if (relTable.size() > 1) {
			// If more than 2 products associated with GR
			setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("GR_WITH_MULTIPLE_PRODUCTS"),
					new Object[] { grItemObj.getName() }));
		} else {
			// If only one product return the product
			final IRow fgProductRow = (IRow) relTable.iterator().next();
			fgProductItem = (IItem) fgProductRow.getReferent();
		}
		return fgProductItem;
	}

	/**
	 * Get GTIN associated with GR
	 * 
	 * @param graphicReq
	 * @return
	 * @throws APIException
	 */
	private IItem getGTINObjectFromGR(final IItem graphicReq) throws APIException {
		final String gtinOnGR = getAttributeValueFromDataObj(graphicReq, ItemConstants.TABLE_REDLINEPAGETHREE,
				graphicRequisitionOrder, PropertiesLoader.getProperty("ATTRID_GTIN_ON_GR"), true);
		final IItem gtinObjFromGR = (IItem) getDataObjectFromString(gtinOnGR.toString(), IItem.OBJECT_TYPE);
		return gtinObjFromGR;
	}

	/**
	 * Validate Ounce weight of GR and GTIN
	 * 
	 * @param graphicReq
	 * @param gtinObj
	 * @return
	 * @throws APIException
	 */
	private boolean validateGRNetWeightWithGTIN(final IItem graphicReq, final IItem gtinObj) throws APIException {
		boolean isValidationSuccess = true;

		final String grOzWeight = getAttributeValueFromDataObj(graphicReq, ItemConstants.TABLE_REDLINEPAGETHREE,
				graphicRequisitionOrder, PropertiesLoader.getProperty("ATTRID_OZ_NTWEIGHT_ON_GR"), true);
		if (gtinObj == null) {
			setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("GR_WITH_NO_GTIN"),
					new Object[] { graphicReq.getName() }));

		} else {
			final Object gtinOzWeight = gtinObj
					.getValue(getBaseIdOrAPIName(PropertiesLoader.getProperty("ATTRID_OZ_NTWEIGHT_ON_GTIN")));
			if (gtinOzWeight == null || "".equalsIgnoreCase(gtinOzWeight.toString())) {
				setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("GTIN_WITH_NO_WEIGHT"),
						new Object[] { gtinObj.getName(), graphicReq.getName() }));

			} else if (grOzWeight == null || "".equalsIgnoreCase(grOzWeight.toString())) {
				setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("GR_WITH_NO_WEIGHT"),
						new Object[] { graphicReq.getName() }));
			} else
				if (Double.compare(Double.parseDouble(gtinOzWeight.toString()), Double.parseDouble(grOzWeight)) != 0) {
				isValidationSuccess = false;
				setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("OZWEIGHT_DIFF_GTIN_GR"),
						new Object[] { graphicReq.getName(), gtinObj.getName() }));

			}
		}

		return isValidationSuccess;
	}

	/**
	 * Perform validations on GRn against the FG Product
	 * 
	 * @param product
	 * @param graphicReq
	 * @throws APIException
	 */
	private void validateGraphicReqWithFGProduct(final IItem product, final IItem graphicReq) throws APIException {

		// Check if GTIN is in sync
		IItem gtinFromProd = null;
		IItem gtinObjFromGR = null;
		// boolean isSuccess =false;
		if (product != null && graphicReq != null) {

			/**@author 09176239
			 * 
			 * validation bypassing if the attribute 'validate trade gtin' is no
			 */
			

			final String validateTradeGTIN = getAttributeValueFromDataObj(graphicReq, ItemConstants.TABLE_REDLINEPAGETHREE,
					graphicRequisitionOrder, "list16", true);
			
			logger.info("Validate Trade GTIN value is : " +validateTradeGTIN);
			
					if(validateTradeGTIN.equals("Yes")){
						
			// Validate attributes of GR and Product sales size flovour
						
						final boolean isSuccess = validateAttributeValues(product, graphicReq,
					PropertiesLoader.getProperty("VALIDATE_ATTRS_PRODUCT_AND_GR"));
			//if (isSuccess) 
			{
				// Get GTIN from GR
				gtinObjFromGR = getGTINObjectFromGR(graphicReq);
				// Get GTIN From Product
				final ITable prodBomTable = product.getTable(ItemConstants.TABLE_BOM)
						.where(PropertiesLoader.getProperty("QUERY_TRADEGTIN_FROM_PRODUCT"), null);
				final Iterator<IRow> bomIter = prodBomTable.iterator();

				while (bomIter.hasNext()) {
					final IRow bomRow = bomIter.next();
					gtinFromProd = (IItem) bomRow.getReferent();
					break;
				}
				// Check if the GTIN values are in SYNC
				if (gtinFromProd != null && gtinObjFromGR != null) {
					if (!gtinFromProd.getName().equalsIgnoreCase(gtinObjFromGR.getName())) {
						setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("GTIN_DIFF_PRODUCT_GR"),
								new Object[] { graphicReq.getName(), product.getName() }));

					} else {
						validateGRNetWeightWithGTIN(graphicReq, gtinObjFromGR);
					}
				} else {
					setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("GTIN_PRODUCT_GR_ISBLANK"),
							new Object[] { graphicReq.getName(), product.getName() }));

				}

			}
		}
		}
	}

	/**
	 * Get all the GR objects which are added as the affected items for the GRO
	 * 
	 * @param graphicRequisitionOrder
	 * @return
	 * @throws APIException
	 */
	private Iterator<IRow> getGRObjectsAssociatedWithGRO(final IChange graphicRequisitionOrder) throws APIException {
		final ITable affItemTable = graphicRequisitionOrder.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		return affItemTable.iterator();
	}

	/**
	 * Validate attributes of product with GR
	 * 
	 * @param srcObj
	 * @param destObj
	 * @param attrsToBeValidated
	 * @return
	 * @throws APIException
	 */
	private boolean validateAttributeValues(final IItem srcObj, final IItem destObj, final String attrsToBeValidated)
			throws APIException {
		logger.info("Execute validateAttributeValues");
		final StringTokenizer tokenizer = new StringTokenizer(attrsToBeValidated, "|");
		boolean isAttrValidationSuccess = true;
		while (tokenizer.hasMoreTokens()) {
			final String attributesListEntry = tokenizer.nextToken();
			logger.info("attributesListEntry - "+attributesListEntry);

			final String[] mapEntry = attributesListEntry.split("#");
			final String attributeName = mapEntry[0];
			final String[] keyValuePair = mapEntry[1].split(":");
			final String srcValue = srcObj.getValue(getBaseIdOrAPIName(keyValuePair[0])).toString();
			final String destValue = getAttributeValueFromDataObj(destObj, ItemConstants.TABLE_REDLINEPAGETHREE,
					graphicRequisitionOrder, keyValuePair[1], true);
			logger.info("Attribute Name-"+attributeName+"Source Value-"+srcValue+";Destination Value-"+destValue);
			if (!srcValue.equalsIgnoreCase(destValue)) {
				isAttrValidationSuccess = false;
				setBannerMessage(MessageFormat.format(PropertiesLoader.getProperty("BDC_DIFF_PRODUCT_GR"),
						new Object[] { attributeName, srcObj.getName(), destObj.getName() }));

				
			}
		}
		return isAttrValidationSuccess;
	}

	public String getFunctionalTeamsfromGRCO() throws APIException {
		String bannerMessageValidation="";
		logger.info("Inside getFunctionalTeamsfromGRCO...");
		GROUpdateFunctionalTeamFromGRAffectedItemsPostPXBO bo = new GROUpdateFunctionalTeamFromGRAffectedItemsPostPXBO();
		Set<String> functionalTeamsGRCOSet = new LinkedHashSet<String>();
		String functionalTeamsGRCO = graphicRequisitionOrder.getValue(PropertiesLoader.getProperty("FUNCTIONAL_TEAM"))
				.toString();
		if (functionalTeamsGRCO != null) {
			StringTokenizer tokenizer = new StringTokenizer(functionalTeamsGRCO, ";");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				functionalTeamsGRCOSet.add(token);
			}
		}
		Set<String> functionalTeamsGRSet = bo.getFunctionalTeamsFromAffectedItemsGR(agileSession,
				graphicRequisitionOrder);
		logger.info("functionalTeamsGRSet length: "+functionalTeamsGRSet.size());
		if(functionalTeamsGRSet.size()>0)
		{
			if (!(functionalTeamsGRCOSet.containsAll(functionalTeamsGRSet))) {
				logger.info("Setting Banner msg");
				bannerMessageValidation="Functional Team in GRCO " + graphicRequisitionOrder.getName()
						+ " is not matched with all Affected Items GR."
						+ " Please run 'Update Functional Team in Graphic Requisition CO' from Actions Menu and try again. ";
			}
		}
		return bannerMessageValidation;

	}

	/**
	 * Check if the input is String or integer and return likewise
	 * 
	 * @param input
	 * @return Object
	 */
	private Object getBaseIdOrAPIName(final String input) {
		Object output = null;
		final String regex = "[0-9]+";
		if (input.matches(regex)) {
			output = Integer.parseInt(input);
		} else {
			output = input;
		}
		return output;
	}
}
