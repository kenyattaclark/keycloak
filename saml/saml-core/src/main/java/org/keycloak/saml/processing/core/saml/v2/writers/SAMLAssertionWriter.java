/*
 * JBoss, Home of Professional Open Source. Copyright 2008, Red Hat Middleware LLC, and individual contributors as
 * indicated by the @author tags. See the copyright.txt file in the distribution for a full listing of individual
 * contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site:
 * http://www.fsf.org.
 */
package org.keycloak.saml.processing.core.saml.v2.writers;

import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.dom.saml.v2.assertion.AdviceType;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextClassRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextDeclRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextDeclType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.ConditionAbstractType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.EncryptedElementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.assertion.URIType;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;

/**
 * Write the SAML Assertion to stream
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 2, 2010
 */
public class SAMLAssertionWriter extends BaseWriter {

    public SAMLAssertionWriter(XMLStreamWriter writer) {
        super(writer);
    }

    /**
     * Write an {@code AssertionType} to stream
     *
     * @param assertion
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public void write(AssertionType assertion) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.ASSERTION.get(), ASSERTION_NSURI.get());
        StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());
        StaxUtil.writeDefaultNameSpace(writer, ASSERTION_NSURI.get());

        // Attributes
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ID.get(), assertion.getID());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.VERSION.get(), assertion.getVersion());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISSUE_INSTANT.get(), assertion.getIssueInstant().toString());

        NameIDType issuer = assertion.getIssuer();
        if (issuer != null)
            write(issuer, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.ISSUER.get(), ASSERTION_PREFIX));

        Element sig = assertion.getSignature();
        if (sig != null)
            StaxUtil.writeDOMElement(writer, sig);

        SubjectType subject = assertion.getSubject();
        if (subject != null) {
            write(subject);
        }

        ConditionsType conditions = assertion.getConditions();
        if (conditions != null) {
            StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.CONDITIONS.get(), ASSERTION_NSURI.get());

            if (conditions.getNotBefore() != null) {
                StaxUtil.writeAttribute(writer, JBossSAMLConstants.NOT_BEFORE.get(), conditions.getNotBefore().toString());
            }

            if (conditions.getNotOnOrAfter() != null) {
                StaxUtil.writeAttribute(writer, JBossSAMLConstants.NOT_ON_OR_AFTER.get(), conditions.getNotOnOrAfter().toString());
            }

            List<ConditionAbstractType> typeOfConditions = conditions.getConditions();
            if (typeOfConditions != null) {
                for (ConditionAbstractType typeCondition : typeOfConditions) {
                    if (typeCondition instanceof AudienceRestrictionType) {
                        AudienceRestrictionType art = (AudienceRestrictionType) typeCondition;
                        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUDIENCE_RESTRICTION.get(),
                                ASSERTION_NSURI.get());
                        List<URI> audiences = art.getAudience();
                        if (audiences != null) {
                            for (URI audience : audiences) {
                                StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUDIENCE.get(),
                                        ASSERTION_NSURI.get());
                                StaxUtil.writeCharacters(writer, audience.toString());
                                StaxUtil.writeEndElement(writer);
                            }
                        }

                        StaxUtil.writeEndElement(writer);
                    }
                }
            }

            StaxUtil.writeEndElement(writer);
        }

        AdviceType advice = assertion.getAdvice();
        if (advice != null)
            throw logger.notImplementedYet("Advice");

        Set<StatementAbstractType> statements = assertion.getStatements();
        if (statements != null) {
            for (StatementAbstractType statement : statements) {
                if (statement instanceof AuthnStatementType) {
                    write((AuthnStatementType) statement);
                } else if (statement instanceof AttributeStatementType) {
                    write((AttributeStatementType) statement);
                } else
                    throw logger.writerUnknownTypeError(statement.getClass().getName());
            }
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    /**
     * Write an {@code StatementAbstractType} to stream
     *
     * @param statement
     *
     * @throws ProcessingException
     */
    public void write(StatementAbstractType statement) throws ProcessingException {
        // TODO: handle this section
        throw logger.notImplementedYet("NYI");
    }

    public void write(AttributeStatementType statement) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.ATTRIBUTE_STATEMENT.get(),
                ASSERTION_NSURI.get());

        List<ASTChoiceType> attributes = statement.getAttributes();
        if (attributes != null) {
            for (ASTChoiceType attr : attributes) {
                AttributeType attributeType = attr.getAttribute();
                if (attributeType != null) {
                    write(attributeType);
                }
                EncryptedElementType encType = attr.getEncryptedAssertion();
                if (encType != null)
                    throw logger.notImplementedYet("EncryptedElementType");
            }
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    /**
     * Write an {@code AuthnStatementType} to stream
     *
     * @param authnStatement
     *
     * @throws ProcessingException
     */
    public void write(AuthnStatementType authnStatement) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHN_STATEMENT.get(), ASSERTION_NSURI.get());

        XMLGregorianCalendar authnInstant = authnStatement.getAuthnInstant();
        if (authnInstant != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.AUTHN_INSTANT.get(), authnInstant.toString());
        }

        String sessionIndex = authnStatement.getSessionIndex();

        if (sessionIndex != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.SESSION_INDEX.get(), sessionIndex);
        }

        AuthnContextType authnContext = authnStatement.getAuthnContext();
        if (authnContext != null)
            write(authnContext);

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    /**
     * Write an {@code AuthnContextType} to stream
     *
     * @param authContext
     *
     * @throws ProcessingException
     */
    public void write(AuthnContextType authContext) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHN_CONTEXT.get(), ASSERTION_NSURI.get());

        AuthnContextType.AuthnContextTypeSequence sequence = authContext.getSequence();
        if (sequence != null) {
            AuthnContextClassRefType authnContextClassRefType = sequence.getClassRef();
            if (authnContextClassRefType != null) {
                StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHN_CONTEXT_CLASS_REF.get(),
                        ASSERTION_NSURI.get());
                StaxUtil.writeCharacters(writer, authnContextClassRefType.getValue().toASCIIString());
                StaxUtil.writeEndElement(writer);
            }

            Set<URIType> uriTypes = sequence.getURIType();
            if (uriTypes != null) {
                for (URIType uriType : uriTypes) {
                    if (uriType instanceof AuthnContextDeclType) {
                        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX,
                                JBossSAMLConstants.AUTHN_CONTEXT_DECLARATION.get(), ASSERTION_NSURI.get());
                        StaxUtil.writeCharacters(writer, uriType.getValue().toASCIIString());
                        StaxUtil.writeEndElement(writer);
                    }
                    if (uriType instanceof AuthnContextDeclRefType) {
                        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX,
                                JBossSAMLConstants.AUTHN_CONTEXT_DECLARATION_REF.get(), ASSERTION_NSURI.get());
                        StaxUtil.writeCharacters(writer, uriType.getValue().toASCIIString());
                        StaxUtil.writeEndElement(writer);
                    }
                }
            }
        }

        Set<URI> authAuthorities = authContext.getAuthenticatingAuthority();
        if (authAuthorities != null) {
            for (URI aa : authAuthorities) {
                StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHENTICATING_AUTHORITY.get(),
                        ASSERTION_NSURI.get());
                StaxUtil.writeCharacters(writer, aa.toASCIIString());
                StaxUtil.writeEndElement(writer);
            }
        }

        Set<URIType> uriTypes = authContext.getURIType();
        for (URIType uriType : uriTypes) {
            if (uriType instanceof AuthnContextClassRefType) {
                StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHN_CONTEXT_CLASS_REF.get(),
                        ASSERTION_NSURI.get());
                StaxUtil.writeCharacters(writer, uriType.getValue().toString());
                StaxUtil.writeEndElement(writer);
            } else if (uriType instanceof AuthnContextDeclRefType) {
                StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHN_CONTEXT_DECLARATION_REF.get(),
                        ASSERTION_NSURI.get());
                StaxUtil.writeCharacters(writer, uriType.getValue().toString());
                StaxUtil.writeEndElement(writer);
            } else if (uriType instanceof AuthnContextDeclType) {
                StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.AUTHN_CONTEXT_DECLARATION.get(),
                        ASSERTION_NSURI.get());
                StaxUtil.writeCharacters(writer, uriType.getValue().toString());
                StaxUtil.writeEndElement(writer);
            }
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }
}