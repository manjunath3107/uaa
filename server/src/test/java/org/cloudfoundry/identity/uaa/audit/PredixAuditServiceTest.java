package org.cloudfoundry.identity.uaa.audit;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ge.predix.audit.sdk.AuditClient;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEnums;
import com.ge.predix.audit.sdk.message.AuditEventV2;

@RunWith(MockitoJUnitRunner.class)
public class PredixAuditServiceTest {

    private static final String IDENTITY_ZONE_ID = "12345678-1234-1234-1234-123456789012";
    private static final String CORRELATION_ID = "abcdeabc-abcd-abcd-abcd-abcdeabcdeab";

    @Mock
    private AuditClient mockAuditClient;

    @InjectMocks
    PredixAuditService auditService = spy(PredixAuditService.class);

    @Test
    public void testAuditNoCorrelationId() {
        AuditEvent auditEvent = new AuditEvent(
                AuditEventType.ClientAuthenticationFailure, "princialID",
                "origin", "data", System.currentTimeMillis(), IDENTITY_ZONE_ID);
        try {
            auditService.log(auditEvent);
            verify(mockAuditClient)
                    .audit(Matchers.argThat(new AuditServiceOutputValidator(AuditEnums.CategoryType.AUTHENTICATIONS,
                            AuditEnums.EventType.LOGIN_FAILURE, AuditEnums.Classifier.FAILURE,
                            "00000000-0000-0000-0000-000000000000",
                            IDENTITY_ZONE_ID)));
        } catch (AuditException e) {
            fail("threw auditException: " + e.getMessage());
        }
    }

    @Test
    public void testAuditNoIdentityZone() {
        AuditEvent auditEvent = new AuditEvent(
                AuditEventType.ClientAuthenticationFailure, "princialID",
                "origin", "data", System.currentTimeMillis(), null);
        when(auditService.getCorrelationId()).thenReturn(CORRELATION_ID);
        try {
            auditService.log(auditEvent);
            verify(mockAuditClient).audit(Matchers
                    .argThat(new AuditServiceOutputValidator(AuditEnums.CategoryType.AUTHENTICATIONS,
                            AuditEnums.EventType.LOGIN_FAILURE, AuditEnums.Classifier.FAILURE,
                            CORRELATION_ID,
                            "00000000-base-uaa0-0000-000000000000")));
        } catch (AuditException e) {
            fail("threw auditException: " + e.getMessage());
        }
    }

    @Test
    public void testAuditIDPDeleted() {
        AuditEvent auditEvent = new AuditEvent(
                AuditEventType.EntityDeletedEvent, "princialID", "origin",
                "('Class:org.cloudfoundry.identity.uaa.provider.IdentityProvider;"
                        + " ID:')",
                System.currentTimeMillis(), IDENTITY_ZONE_ID);
        when(auditService.getCorrelationId()).thenReturn(CORRELATION_ID);
        try {
            auditService.log(auditEvent);
            verify(mockAuditClient)
                    .audit(Matchers.argThat(new AuditServiceOutputValidator(AuditEnums.CategoryType.AUTHORIZATION,
                            AuditEnums.EventType.ACCOUNT_PRIVILEGE_SUCCESS_MODIFICATION, AuditEnums.Classifier.SUCCESS,
                            CORRELATION_ID, IDENTITY_ZONE_ID)));
        } catch (AuditException e) {
            fail("threw auditException: " + e.getMessage());
        }
    }

    @Test
    public void testAuditIdentityZoneDeleted() {
        AuditEvent auditEvent = new AuditEvent(
                AuditEventType.EntityDeletedEvent, "princialID", "origin",
                "('Class:org.cloudfoundry.identity.uaa.zone.IdentityZone; ID:')",
                System.currentTimeMillis(), IDENTITY_ZONE_ID);
        when(auditService.getCorrelationId()).thenReturn(CORRELATION_ID);
        try {
            auditService.log(auditEvent);
            verify(mockAuditClient)
                    .audit(Matchers.argThat(new AuditServiceOutputValidator(AuditEnums.CategoryType.ADMINISTRATIONS,
                            AuditEnums.EventType.CHANGE_CONFIGURATIONS_SUCCESS, AuditEnums.Classifier.SUCCESS,
                            CORRELATION_ID, IDENTITY_ZONE_ID)));
        } catch (AuditException e) {
            fail("threw auditException: " + e.getMessage());
        }
    }

    private class AuditServiceOutputValidator
            extends
                ArgumentMatcher<AuditEventV2> {
        private String correlationId;
        private String identityZoneId;
        private AuditEnums.Classifier status;
        private AuditEnums.EventType eventType;
        private AuditEnums.CategoryType categoryType;

        public AuditServiceOutputValidator(AuditEnums.CategoryType categoryType, AuditEnums.EventType eventType,
                AuditEnums.Classifier status, String correlationId, String identityZoneId) {
            this.correlationId = correlationId;
            this.identityZoneId = identityZoneId;
            this.status = status;
            this.eventType = eventType;
            this.categoryType = categoryType;
        }

        @Override
        public boolean matches(Object event) {
            AuditEventV2 actualAuditEvent = (AuditEventV2) event;
            return actualAuditEvent.getCategoryType() == categoryType &&
                    actualAuditEvent.getEventType() == eventType &&
                    actualAuditEvent.getClassifier() == status &&
                    actualAuditEvent.getTenantUuid() == identityZoneId && 
                    actualAuditEvent.getCorrelationId() == correlationId;
        }
        public String toString() {
            // printed in verification errors
            return "audit service did not match expected";
        }
    }

}
