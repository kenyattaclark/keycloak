package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_3_0_Beta1 extends Update {

    @Override
    public String getId() {
        return "1.3.0.Beta1";
    }

    @Override
    public void update(KeycloakSession session) {
        deleteEntries("clientSessions");
        deleteEntries("sessions");

        removeField("realms", "passwordCredentialGrantAllowed");

        updateIdentityProviders();
    }

    private void updateIdentityProviders() {
        DBCollection realms = db.getCollection("realms");
        DBCursor realmsCursor = realms.find();

        try {
            while (realmsCursor.hasNext()) {
                BasicDBObject realm = (BasicDBObject) realmsCursor.next();

                BasicDBList identityProviders = (BasicDBList) realm.get("identityProviders");
                if (identityProviders != null) {
                    for (Object ipObj : identityProviders) {
                        BasicDBObject identityProvider = (BasicDBObject) ipObj;

                        boolean updateProfileFirstLogin = identityProvider.getBoolean("updateProfileFirstLogin");
                        String upflMode = updateProfileFirstLogin ? IdentityProviderRepresentation.UPFLM_ON : IdentityProviderRepresentation.UPFLM_OFF;
                        identityProvider.put("updateProfileFirstLoginMode", upflMode);
                        identityProvider.removeField("updateProfileFirstLogin");

                        identityProvider.put("trustEmail", false);
                    }
                }

                realms.save(realm);
            }
        } finally {
            realmsCursor.close();
        }
    }

}
