package com.pepsico.agile.events;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.CommonConstants;
import com.agile.api.IAgileList;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.IUser;
import com.agile.api.ItemConstants;
import com.agile.api.UserConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.events.beans.NotificationBean;
import com.pepsico.agile.px.util.PropertiesLoader;

public class ValidationsBeforeUpdatingMarketingCopyDescriptionFromBrandMarkDescriptionPrePX
    implements IEventAction
{

    public static final Logger logger = Logger.getLogger(ValidationsBeforeUpdatingMarketingCopyDescriptionFromBrandMarkDescriptionPrePX.class.getName());
    final NotificationBean     bean   = new NotificationBean();


    @Override

    public EventActionResult doAction(IAgileSession session,
                                      INode node,
                                      IEventInfo eventInfo)
    {
        ActionResult actionResult = null;
        IItem grItem = null;
        // TODO Auto-generated method stub

        try
        {
            PropertiesLoader.loadResource("CornerStone");
            DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));

            IWFChangeStatusEventInfo info = (IWFChangeStatusEventInfo) eventInfo;
            IChange gco = (IChange) info.getDataObject();
            String grListValue = null;
            logger.info("Change Order " + gco);

            grItem = getGRObject(gco);
            ITable p3RedlineTable = grItem.getTable(ItemConstants.TABLE_REDLINEPAGETHREE);

            Iterator p3RedlineTableIterator = p3RedlineTable.getTableIterator();
            while (p3RedlineTableIterator.hasNext())
            {
                IRow redPage2Row = (IRow) p3RedlineTableIterator.next();

                ICell cell = redPage2Row.getCell(CommonConstants.ATT_PAGE_THREE_LIST14);
                grListValue = getListValueUsingCell(cell, grItem);

                logger.info("GR Type " + grListValue);

                if (Constants.GR_TYPE_LIST_VALUES.contains(grListValue))
                {
                    IItem brandMark = getBrandMarkObject(grItem);
                    logger.info("Brand Mark: " + brandMark);
                    if (brandMark != null)
                    {
                        String lcpBrandMark = getListValue(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE,
                                                           brandMark);
                        if (!(lcpBrandMark.equals(Constants.PENDING_LCP) ||
                              lcpBrandMark.equals(Constants.PRODUCTION_LCP)))
                        {
                            String returnValue = brandMarkLifeCyclePhaseIsNotPendingProd(session,
                                                                                         gco,
                                                                                         grItem,
                                                                                         brandMark);
                            if (returnValue.isEmpty())
                            {
                                actionResult = new ActionResult(ActionResult.STRING,
                                                                Constants.SUCCESS_MESSAGE);

                            }
                            else
                            {
                                actionResult = new ActionResult(ActionResult.EXCEPTION,
                                                                new Exception(returnValue));
                                bean.setNotificationSubject(Constants.GRCO_AUTOPROMOTION_FAILURE_SUBJECT);
                                bean.setNotificationBody("Validation Failed: " + returnValue);
                                sendNotificationToOriginator(bean, gco);

                            }
                        }
                        else
                        {
                            String returnValue2 = brandMarkLifeCyclePhaseIsPendingProd(gco,
                                                                                       grItem,
                                                                                       brandMark);
                            if (returnValue2.equalsIgnoreCase(Constants.MARKETING_COPY_PRODUCTION_LIFECYCLE_MSG))
                            {
                                actionResult = new ActionResult(ActionResult.STRING,
                                                                Constants.MARKETING_COPY_PRODUCTION_LIFECYCLE_MSG);

                            }
                            else if (returnValue2.isEmpty())
                            {
                                actionResult = new ActionResult(ActionResult.STRING,
                                                                Constants.SUCCESS_MESSAGE);
                            }
                            else
                            {
                                actionResult = new ActionResult(ActionResult.EXCEPTION,
                                                                new Exception(returnValue2));
                                bean.setNotificationSubject(Constants.GRCO_AUTOPROMOTION_FAILURE_SUBJECT);
                                bean.setNotificationBody("Validation Failed: " + returnValue2);
                                sendNotificationToOriginator(bean, gco);

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
                        ;
                    }
                }
                else
                {
                    actionResult = new ActionResult(ActionResult.STRING,
                                                    MessageFormat.format(Constants.GR_TYPE_NOT_PRESENT,
                                                                         new Object[] {
                                                                                        gco,
                                                                                        grListValue
                                                                         }));
                    ;
                }
            }
        }
        catch (APIException e)
        {
            logger.error("", e);
            actionResult = new ActionResult(ActionResult.STRING,
                                            MessageFormat.format(Constants.MARKETING_COPY_DESC_ERROR_MSG,
                                                                 new Object[] {
                                                                                grItem
                                                                 }));
            ;
        }
        catch (MessagingException e)
        {
            logger.error("", e);
            actionResult = new ActionResult(ActionResult.STRING, Constants.MAIL_EXCEPTION_MESSAGE);

            ;
        }
        return new EventActionResult(eventInfo, actionResult);
    }


    /**
     * Get the Graphic Requisition Item object
     * 
     * @param GCO
     * @return grItem
     * @throws APIException
     */
    public IItem getGRObject(IChange GCO)
        throws APIException
    {
        IItem grItem = null;
        ITable affectedItem;
        logger.info("****Inside get GR Item****");
        affectedItem = GCO.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
        Iterator affectedItemIterator = affectedItem.getTableIterator();
        while (affectedItemIterator.hasNext())
        {
            IRow row = (IRow) affectedItemIterator.next();
            IItem affctdItem = (IItem) row.getReferent();
            String partType = affctdItem.getValue(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE).toString();
            if (partType.equals(Constants.GRAPHIC_REQUISITION))
            {
                grItem = affctdItem;
            }
        }
        logger.info("GR Item " + grItem);
        return grItem;
    }


    /**
     * Get the Brand Mark Item object
     * 
     * @param grItem
     * @return brandMarkObject
     * @throws APIException
     */
    public IItem getBrandMarkObject(IItem grItem)
        throws APIException
    {
        IItem brandMarkObject = null;

        logger.info("****Inside get BrandMark Item****");
        ITable tableRelatedProduct = grItem.getTable(ItemConstants.TABLE_RELATIONSHIPS);
        Iterator itr = tableRelatedProduct.iterator();
        while (itr.hasNext())
        {
            IRow rowRelatedProduct = (IRow) itr.next();
            String relatedType = rowRelatedProduct.getCell(ItemConstants.ATT_RELATIONSHIPS_TYPE).toString();
            if (relatedType.equals(Constants.BRAND_MARK))
            {
                brandMarkObject = (IItem) rowRelatedProduct.getReferent();

            }
        }
        logger.info("BrandMark Item " + brandMarkObject);
        return brandMarkObject;

    }


    /**
     * Get the List Value
     * 
     * @param attrID
     * @param item
     * @return returnValue
     * @throws APIException
     */
    public String getListValue(Integer attrID,
                               IItem item)
        throws APIException
    {

        String value = "";
        ICell cell = item.getCell(attrID);
        IAgileList cl = (IAgileList) cell.getValue();

        IAgileList[] selected = cl.getSelection();
        if (selected != null && selected.length > 0)
        {
            value = (selected[0].getValue()).toString();
        }
        return value;

    }


    /**
     * Get the List Value
     * 
     * @param attrID
     * @param item
     * @return returnValue
     * @throws APIException
     */
    public String getListValueUsingCell(ICell cell,
                                        IItem item)
        throws APIException
    {

        String value = "";
        IAgileList cl = (IAgileList) cell.getValue();

        IAgileList[] selected = cl.getSelection();
        if (selected != null && selected.length > 0)
        {
            value = (selected[0].getValue()).toString();
        }
        return value;

    }


    /**
     * Return result if Brand MArk is not in Pending or Production LifeCycle Phase
     * 
     * @param GCO
     * @param grItem
     * @return returnValue
     * @throws APIException
     * @throws MessagingException
     */
    public String brandMarkLifeCyclePhaseIsNotPendingProd(IAgileSession session,
                                                          IChange gco,
                                                          IItem grItem,
                                                          IItem brandMark)
        throws APIException, MessagingException
    {
        logger.info("****Inside Brand Mark Lifecycle Phase is not Pending or PRoduction****");
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
            String bomtype = getListValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE, bomItem);
            if (bomtype.equalsIgnoreCase(Constants.MARKETING_COPY))
            {
                mcValue = true;
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
        logger.info("****Inside Brand Mark Lifecycle Phase is  Pending or PRoduction****");
        String brandMarkDescription = (String) brandMark.getValue(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION);
        String returnValue = "";
        ITable bomTable = grItem.getTable(ItemConstants.TABLE_BOM);
        Iterator itr = bomTable.iterator();
        boolean mcValue = false;
        while (itr.hasNext())
        {
            IRow row = (IRow) itr.next();
            IItem bomItem = (IItem) row.getReferent();
            String bomtype = getListValue(ItemConstants.ATT_TITLE_BLOCK_ITEM_TYPE, bomItem);
            logger.info("BOM Type: " + bomtype);

            if (bomtype.equals(Constants.MARKETING_COPY))
            {
                mcValue = true;
                logger.info(mcValue);
                String marketingCopyLCP = getListValue(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE,
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

        logger.info("Return Value" + returnValue);
        return returnValue;
    }


    /**
     * Send notification to the grco originator if pre validation fails.
     * 
     * @param bean
     * @param bpco
     * @throws MessagingException
     * @throws APIException
     */
    public void sendNotificationToOriginator(final NotificationBean bean,
                                             final IChange grco)
        throws MessagingException, APIException
    {
        if (!bean.getNotificationBody().isEmpty())
        {
            final IUser currentUser = (IUser) grco.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getReferent();
            final String emailAddress = currentUser.getValue(UserConstants.ATT_GENERAL_INFO_EMAIL).toString();
            final Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", PropertiesLoader.getProperty("Mail.Hostname"));
            final Session session = Session.getDefaultInstance(properties);
            final MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(PropertiesLoader.getProperty("Mail.FromAddress")));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
            message.setSubject(bean.getNotificationSubject());
            final StringBuilder content = new StringBuilder();
            content.append(bean.getNotificationBody());
            message.setContent(content.toString(), "text/html");
            Transport.send(message);
        }
    }

}
