package com.pepsico.agile.events;

import java.text.MessageFormat;
import java.util.Iterator;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

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
import com.pepsico.agile.events.beans.NotificationBean;
import com.pepsico.agile.px.util.PropertiesLoader;

public class UpdateMarketingCopyDescriptionFromBrandMarkDescriptionPostPX implements IEventAction
{

    public static final Logger                                                     logger     = Logger.getLogger(ValidationsBeforeUpdatingMarketingCopyDescriptionFromBrandMarkDescriptionPrePX.class.getName());

    ValidationsBeforeUpdatingMarketingCopyDescriptionFromBrandMarkDescriptionPrePX validatePX = new ValidationsBeforeUpdatingMarketingCopyDescriptionFromBrandMarkDescriptionPrePX();
    NotificationBean                                                               bean       = new NotificationBean();


    @Override
    public EventActionResult doAction(IAgileSession session,
                                      INode node,
                                      IEventInfo eventInfo)
    {
        // TODO Auto-generated method stub

        IWFChangeStatusEventInfo info = (IWFChangeStatusEventInfo) eventInfo;
        ActionResult actionResult = null;
        IChange gco = null;
        try
        {
            gco = (IChange) info.getDataObject();
            logger.info("Change Order " + gco);
            IItem grItem = validatePX.getGRObject(gco);
            logger.info("GR " + grItem);
            IItem brandMarkItem = validatePX.getBrandMarkObject(grItem);
            logger.info("Brand Mark: " + brandMarkItem);
            if (brandMarkItem != null)
            {
                ITable bomTable = grItem.getTable(ItemConstants.TABLE_BOM);
                Iterator itr = bomTable.iterator();
                while (itr.hasNext())
                {
                    IRow row = (IRow) itr.next();
                    IItem bomItem = (IItem) row.getReferent();
                    
                    String bomtype = validatePX.getListValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE,
                                                             bomItem);
                    String brandMarkLifeCyclePhase = validatePX.getListValue(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE,
                                                                             brandMarkItem);
                    if (!(brandMarkLifeCyclePhase.equals(Constants.PENDING_LCP) ||
                          brandMarkLifeCyclePhase.equals(Constants.PRODUCTION_LCP)))
                    {
                        String returnValue = brandMarkLifeCyclePhaseIsNotPendingProd(session,
                                                                                     gco,
                                                                                     grItem,
                                                                                     brandMarkItem);
                        if (returnValue.isEmpty())
                        {
                            actionResult = new ActionResult(ActionResult.STRING,
                                                            Constants.MARKETING_COPY_DESC_SUCCESS_MSG);

                        }
                        else
                        {
                            actionResult = new ActionResult(ActionResult.EXCEPTION,
                                                            new Exception(returnValue));
                            bean.setNotificationSubject(Constants.GRCO_AUTOPROMOTION_FAILURE_SUBJECT);
                            bean.setNotificationBody("Validation Failed: " + returnValue);
                            validatePX.sendNotificationToOriginator(bean, gco);

                        }
                    }
                    else
                    {
                        String returnValue2 = brandMarkLifeCyclePhaseIsPendingProd(gco,
                                                                                   grItem,
                                                                                   brandMarkItem);
                        if (returnValue2.equalsIgnoreCase(Constants.MARKETING_COPY_PRODUCTION_LIFECYCLE_MSG))
                        {
                            actionResult = new ActionResult(ActionResult.STRING,
                                                            Constants.MARKETING_COPY_PRODUCTION_LIFECYCLE_MSG);

                        }
                        else if (returnValue2.isEmpty())
                        {
                            actionResult = new ActionResult(ActionResult.STRING,
                                                            Constants.MARKETING_COPY_DESC_SUCCESS_MSG);
                        }
                        else
                        {
                            actionResult = new ActionResult(ActionResult.EXCEPTION,
                                                            new Exception(returnValue2));
                            bean.setNotificationSubject(Constants.GRCO_AUTOPROMOTION_FAILURE_SUBJECT);
                            bean.setNotificationBody("Validation Failed: " + returnValue2);
                            validatePX.sendNotificationToOriginator(bean, gco);

                        }
                    }
                }
                
            }
            else
            {
                actionResult = new ActionResult(ActionResult.STRING,
                                                MessageFormat.format(Constants.BRANDMARK_ERROR_MSG,
                                                                     new Object[] {
                                                                                    grItem
                                                                     }));
            }
        }
        catch (APIException | MessagingException e)
        {

            actionResult = new ActionResult(ActionResult.EXCEPTION,
                                            new Exception(MessageFormat.format(Constants.MARKETING_COPY_DESC_ERROR_MSG,
                                                                               new Object[] {
                                                                                              gco
                                                                               })));
        }

        return new EventActionResult(eventInfo, actionResult);
    }


    public String brandMarkLifeCyclePhaseIsNotPendingProd(IAgileSession session,
                                                          IChange gco,
                                                          IItem grItem,
                                                          IItem brandMark)
        throws APIException, MessagingException
    {
        logger.info("****Inside BRandMark LifeCycle Phase is not Pending or PRoduction****");
        String brandMarkDescription = (String) brandMark.getValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION);
        String returnValue = "";
        boolean mcValue = false;
        ITable bomTable = grItem.getTable(ItemConstants.TABLE_BOM);
        Iterator itr = bomTable.iterator();
        while (itr.hasNext())
        {
            IRow row = (IRow) itr.next();
            IItem bomItem = (IItem) row.getReferent();
            // IItem bomItem = (IItem)session.getObject(IItem.OBJECT_TYPE,
            // bomItem1.getName());
            String bomtype = validatePX.getListValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE, bomItem);
            if (bomtype.equalsIgnoreCase(Constants.MARKETING_COPY))
            {
                mcValue = true;
                bomItem.refresh();
                bomItem.setValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION, brandMarkDescription);
                String marketingCopyDescription = (String) bomItem.getValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION);

                if (marketingCopyDescription.isEmpty())
                {

                    returnValue = MessageFormat.format(Constants.MARKETING_COPY_DESC_ERROR_MSG, new Object[] {
                                                                                                               gco
                    });
                }
            }
        }
        if (mcValue == false)
        {
            returnValue = MessageFormat.format(Constants.MARKETING_COPY_ERROR_MSG, new Object[] {
                                                                                                  gco
            });
        }

        return returnValue;
    }


    /**
     * Return result if Brand MArk is in Pending or Production LifeCycle Phase
     * 
     * @param GCO
     * @param grItem
     * @return returnValue
     * @throws APIException
     */

    public String brandMarkLifeCyclePhaseIsPendingProd(IChange gco,
                                                       IItem grItem,
                                                       IItem brandMark)
        throws APIException
    {
        logger.info("****Inside Brand Mark LifeCycle Phase is  Pending or PRoduction****");
        String brandMarkDescription = (String) brandMark.getValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION);
        String returnValue = "";
        ITable bomTable = grItem.getTable(ItemConstants.TABLE_BOM);
        Iterator itr = bomTable.iterator();
        boolean mcValue = false;
        while (itr.hasNext())
        {
            IRow row = (IRow) itr.next();
            IItem bomItem = (IItem) row.getReferent();
            String bomtype = validatePX.getListValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE, bomItem);

            if (bomtype.equals(Constants.MARKETING_COPY))
            {
                mcValue = true;
                String marketingCopyLCP = validatePX.getListValue(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE,
                                                                  bomItem);
                if (marketingCopyLCP.equals(Constants.PRODUCTION_LCP))
                {
                    String marketingCopyDescription = (String) bomItem.getValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION);
                    if (!brandMarkDescription.equals(marketingCopyDescription))
                    {
                        returnValue = MessageFormat.format(Constants.MARKETING_COPY_LCP_ERROR_MSG,
                                                           new Object[] {
                                                                          gco
                                                           });
                    }
                    else
                    {
                        returnValue = Constants.MARKETING_COPY_PRODUCTION_LIFECYCLE_MSG;
                    }

                }
                else
                {

                    bomItem.refresh();
                    bomItem.setValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION, brandMarkDescription);
                    String marketingCopyDescription = (String) bomItem.getValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION);

                    if (marketingCopyDescription.isEmpty())
                    {
                        returnValue = MessageFormat.format(Constants.MARKETING_COPY_DESC_ERROR_MSG,
                                                           new Object[] {
                                                                          gco
                                                           });
                    }
                }
            }
        }
        if (mcValue == false)
        {
            returnValue = MessageFormat.format(Constants.MARKETING_COPY_ERROR_MSG, new Object[] {
                                                                                                  gco
            });
        }

        logger.info("Return value " + returnValue);
        return returnValue;
    }

}
