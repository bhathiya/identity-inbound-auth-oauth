/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.dcr.processor;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.inbound.IdentityMessageContext;
import org.wso2.carbon.identity.application.authentication.framework.inbound.IdentityProcessor;
import org.wso2.carbon.identity.application.authentication.framework.inbound.IdentityRequest;
import org.wso2.carbon.identity.application.authentication.framework.inbound.IdentityResponse;
import org.wso2.carbon.identity.oauth.dcr.DCRException;
import org.wso2.carbon.identity.oauth.dcr.context.DCRMessageContext;
import org.wso2.carbon.identity.oauth.dcr.exception.RegistrationException;
import org.wso2.carbon.identity.oauth.dcr.exception.UnRegistrationException;
import org.wso2.carbon.identity.oauth.dcr.handler.RegistrationHandler;
import org.wso2.carbon.identity.oauth.dcr.handler.UnRegistrationHandler;
import org.wso2.carbon.identity.oauth.dcr.model.RegistrationRequest;
import org.wso2.carbon.identity.oauth.dcr.model.UnregistrationRequest;
import org.wso2.carbon.identity.oauth.dcr.util.DCRConstants;
import org.wso2.carbon.identity.oauth.dcr.util.DCRExceptionBuilder;
import org.wso2.carbon.identity.oauth.dcr.util.ErrorCodes;
import org.wso2.carbon.identity.oauth.dcr.util.HandlerManager;

import java.util.regex.Matcher;

public class DCRProcessor extends IdentityProcessor {

    private static Log log = LogFactory.getLog(DCRProcessor.class);

    @Override
    public IdentityResponse.IdentityResponseBuilder process(IdentityRequest identityRequest) throws DCRException {

        if (log.isDebugEnabled()) {
            log.debug("Request processing started by DCRProcessor.");
        }
        DCRMessageContext dcrMessageContext = new DCRMessageContext(identityRequest);
        IdentityResponse.IdentityResponseBuilder identityResponseBuilder = null;
        if (identityRequest instanceof RegistrationRequest) {
            identityResponseBuilder = registerOAuthApplication(dcrMessageContext);
        } else if (identityRequest instanceof UnregistrationRequest) {
            identityResponseBuilder = unRegisterOAuthApplication(dcrMessageContext);
        }
        return identityResponseBuilder;
    }

    @Override
    public String getCallbackPath(IdentityMessageContext context) {
        return null;
    }

    @Override
    public String getRelyingPartyId() {
        return null;
    }

    protected IdentityResponse.IdentityResponseBuilder registerOAuthApplication(DCRMessageContext dcrMessageContext)
            throws RegistrationException {

        IdentityResponse.IdentityResponseBuilder identityResponseBuilder = null;

        try {
            RegistrationHandler registrationHandler =
                    HandlerManager.getInstance().getRegistrationHandler(dcrMessageContext);
            identityResponseBuilder = registrationHandler.handle(dcrMessageContext);
        } catch (DCRException e) {
            if (e.getErrorInfoList().size() == 0) {
                throw DCRExceptionBuilder
                        .buildException(new RegistrationException(e.getErrorMessage()), ErrorCodes.BAD_REQUEST
                                .toString(), e.getErrorMessage());
            } else {
                throw DCRExceptionBuilder.buildException(RegistrationException.class, e.getErrorInfoList()
                        .get(e.getErrorInfoList().size() - 1));
            }
        }
        return identityResponseBuilder;
    }

    protected IdentityResponse.IdentityResponseBuilder unRegisterOAuthApplication(DCRMessageContext dcrMessageContext)
            throws DCRException {
        IdentityResponse.IdentityResponseBuilder identityResponseBuilder = null;
        try {
            UnRegistrationHandler unRegistrationHandler =
                    HandlerManager.getInstance().getUnRegistrationHandler(dcrMessageContext);
            identityResponseBuilder = unRegistrationHandler.handle(dcrMessageContext);
        } catch (DCRException e) {
            if (StringUtils.isBlank(e.getCode())) {
                throw DCRExceptionBuilder
                        .buildException(new UnRegistrationException(e.getErrorMessage()), ErrorCodes.BAD_REQUEST
                                .toString(), e.getErrorMessage());
            } else {
                throw DCRExceptionBuilder.buildException(UnRegistrationException.class, e.getErrorInfoList()
                        .get(e.getErrorInfoList().size()));
            }
        }
        return identityResponseBuilder;
    }

    @Override
    public boolean canHandle(IdentityRequest identityRequest) {
        boolean canHandle = false;
        if (identityRequest != null) {
            Matcher registerMatcher =
                    DCRConstants.DCR_ENDPOINT_REGISTER_URL_PATTERN.matcher(identityRequest.getRequestURI());
            Matcher unRegisterMatcher =
                    DCRConstants.DCR_ENDPOINT_UNREGISTER_URL_PATTERN.matcher(identityRequest.getRequestURI());
            if (registerMatcher.matches() || unRegisterMatcher.matches()) {
                canHandle = true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("canHandle " + canHandle + " by DCRProcessor.");
        }
        return canHandle;
    }
}
