package com.pepsico.agile.events.bo;

import java.util.Iterator;
import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;

import com.agile.api.IChange;

import com.agile.api.IItem;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ItemConstants;

/**
 * @author 09158710
 * @see This PX does the following.
 * @see As part of validations done from moving GRO to Approvals status,
 *      UPC A (12 Digit Bar Code) gets auto populated when Item Reference Number ("Trade GTIN" field)is
 *      redlined. All Validations before moving the status of a GRO from
 *      Marketing services to Approvals status should be satisfied.
 */

public class GROUPCMappingItemRefNumber {

	private static final Logger logger = Logger.getLogger(GROUPCMappingItemRefNumber.class);

	public final int UPC_A_Trade_GTIN = 1588;
	public final int UPC_A_Trade_GTIN_GRAPHIC_REQ = 1580;
	public final int ITEM_REFERENCE_NUMBER = 1556;
	public final String GRAPHIC_REQUISITION = "Graphic Requisition";
	String itemReferenceNumber = "";

	public void UpdateTitleBlockUPC(IAgileSession session, IChange changeObj) {

		try {
			changeObj = (IChange) session.getObject(IChange.OBJECT_TYPE, changeObj.getName());
			logger.info("Graphic Req Order" + changeObj);

			ITable affectedGR = changeObj.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			logger.info("Affected Graphic Req:" + affectedGR);

			@SuppressWarnings("rawtypes")
			Iterator affectedGRItr = affectedGR.iterator();
			logger.info("Before Redlining UA12");
			while (affectedGRItr.hasNext()) {
				IRow affItemRow = (IRow) affectedGRItr.next();

				IItem graphicRequestItem = (IItem) affItemRow.getReferent();
				logger.info(affItemRow.getName() + " is being redlined");

				// get redlined Item reference number
				ITable graphicReqRedlineTableItemRef = graphicRequestItem
						.getTable(ItemConstants.TABLE_REDLINEPAGETHREE);
				@SuppressWarnings("rawtypes")
				Iterator graphicReqRedlineTableItemRefItr = graphicReqRedlineTableItemRef.iterator();
				if (graphicReqRedlineTableItemRefItr.hasNext()) {
					IRow graphicReqRedlineTableItemRefItrRow = (IRow) graphicReqRedlineTableItemRefItr.next();
					itemReferenceNumber = graphicReqRedlineTableItemRefItrRow.getValue(ITEM_REFERENCE_NUMBER)
							.toString();

					if (!itemReferenceNumber.isEmpty()) {
						IItem tradeGTIN = (IItem) session.getObject("Trade GTIN", itemReferenceNumber);
						logger.info("item reference number:" + itemReferenceNumber);

						String upcaTradeGTIN = tradeGTIN.getValue(UPC_A_Trade_GTIN).toString();
						ITable GraphicReqRedlineTable = graphicRequestItem
								.getTable(ItemConstants.TABLE_REDLINEPAGETHREE);
						@SuppressWarnings("unchecked")
						Iterator<IItem> GraphicReqRedlineTableItr = GraphicReqRedlineTable.iterator();
						if (GraphicReqRedlineTableItr.hasNext()) {
							IRow GraphicReqRedlineTableRow = (IRow) GraphicReqRedlineTableItr.next();
							GraphicReqRedlineTableRow.setValue(UPC_A_Trade_GTIN_GRAPHIC_REQ,
									upcaTradeGTIN);
						}
					}
				}
			}
		} catch (APIException e) {
			logger.error("", e);

		}

	}
}
