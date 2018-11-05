package com.pepsico.agile.events;

import java.util.Arrays;
import java.util.List;

public class Constants
{

    public static final String          APINAME_DESIGNARTWORKREQUIRED            = "designArtworkRequired";
    public static final Integer         BASEID_FINALARTWORKRECIEVEDDATE          = 1535;
    public static final String          NOT_REQUIRED                             = "Not Required";
    public static final String          YES_EXISTING                             = "Yes - Existing";
    public static final String          YES_NEW                                  = "Yes - New";
    public static final String          MSG_MADATORY_FINALARTWORKRECIEVEDDATE    = "{0} : 'Final Approved Artwork Received Date' is mandatory.";
    public static final String          FAILURE_MESSAGE                          = "Validation Failed due to error.Please contact agile administrator for more information.";
    public static final String          SUCCESS_MESSAGE                          = "Validation Successful.";
    public static final String          MSG_MANDATORY_BOM_MISSING                = "<p><b>{0}</b>: Affected Item must have a Label Specification object associated to it.</p>";
    public static final String          MSG_MANDATORY_COUNT_STATEMENTSTATUS      = "<p><b>{0}</b>: Attribute values of “BOM.Count” and “BOM.Statement Status” of affected item cannot be null for Label Specification object.</p>";
    public static final String          MSG_SUCCESS_BOM_VALIDATION               = "BOM validation in WF is successful.";
    public static final String          SUBCLASS_LABEL_SPECIFICATION             = "Label Specification";
    public static final String          NOTIFICATIONBODY_FEATUREBENEFIT_MISMATCH = "Validation Failed:  Graphic Requisition Change Order is not able to Validate the claims being made on the Marketing Copy.";
    public static final String          GR_REQUEST_TYPE                          = "Page Three.Graphic Request Type";
    public static final String          ALLOWED_GR_REQUEST_TYPE                  = "Corrugate (Non-Take Home) Only;Flex Innovation Mock Up;Flex Trade Call;MockUp;Polybag";
    public static final Integer         BASEID_CLAIMSBEINGMADE                   = 2000003300;
    public static final String          SUBCLASS_MARKETINGCOPY                   = "Marketing Copy";
    public static final String          SUBCLASS_COLOR_BOX                       = "Color/PMS/FL#/Type";
    public static final String          USAGETYPE_PARENT                         = "Parent - Separation Combo";
    public static final String          USAGETYPE_CHILD                          = "Child - Color Component";
    public static final String          LIFECYCLEPHASE_PRODUCTION                = "Production";
    public static final String          NEW_REV                                  = "01";
    public static final String          SUBCLASS_MARKETING_CHANGE_ORDER          = "Marketing CO";
    public static final String          WORKFLOW_MARKETING                       = "Marketing Workflow";
    public static final Integer         BASEID_BOM_USAGETYPE                     = 2000019044;
    public static final String          MARKETINGCO_NOTCREATED                   = "Marketing CO not released";

    public static final String          GRAPHIC_REQUISITION                      = "Graphic Requisition";
    public static final String          BRAND_MARK                               = "Brand Mark";
    public static final String          MARKETING_COPY                           = "Marketing Copy";

    protected static final List<String> GR_TYPE_LIST_VALUES                      = Arrays.asList("Canister",
                                                                                                 "Container",
                                                                                                 "Corrugated",
                                                                                                 "Cup",
                                                                                                 "Flex Live Film",
                                                                                                 "Folding Carton",
                                                                                                 "Label",
                                                                                                 "Lid",
                                                                                                 "Lidstock");
    public static final String          PENDING_LCP                              = "Pending";
    public static final String          PRODUCTION_LCP                           = "Production";

    public static final String          BRANDMARK_ERROR_MSG                      = "Brand Mark object is not present in the Relationship table of the GR {0}";
    public static final String          MARKETING_COPY_DESC_SUCCESS_MSG          = "Marketing Copy Description updated successfully";
    public static final String          MARKETING_COPY_DESC_ERROR_MSG            = "{0} has failed to auto promote because the Marketing Copy description is not updated.";
    public static final String          MARKETING_COPY_ERROR_MSG                 = "{0} has failed to auto promote because the Marketing Copy is not available.";
    public static final String          MARKETING_COPY_LCP_ERROR_MSG             = "{0} has failed to auto promote because the Production Marketing Copy description is not matching with the Pending/Production Brand Mark";

    public static final String          MARKETING_COPY_PRODUCTION_LIFECYCLE_MSG  = "BrandMark and Marketing Copy are in Production Lifecycle Phase and the decsription of both the objects are same. So the Decription of Marketing Copy is not updated";
    public static final String          GRCO_AUTOPROMOTION_FAILURE_SUBJECT       = "Validation Failed: Graphic Requisition Change Order is not able to move to Next status in the Workflow";
    public static final String          MAIL_EXCEPTION_MESSAGE                   = "Mail has not been sent to Originator";
    public static final String          GR_TYPE_NOT_PRESENT                      = "Marketing Copy decsription updation from Brandmark description is not applicable to the GRCO {0} as the GR type value is {1}";

}
