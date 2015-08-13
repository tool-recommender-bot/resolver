package org.jboss.shrinkwrap.resolver.impl.maven;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.jboss.shrinkwrap.resolver.api.InvalidConfigurationFileException;
import org.jboss.shrinkwrap.resolver.api.maven.MavenWorkingSession;
import org.jboss.shrinkwrap.resolver.impl.maven.bootstrap.MavenRepositorySystem;

/**
 * Configurable implementation of a {@link MavenWorkingSession}, encapsulating Maven/Aether backend.
 * <br/>
 * This is an abstract class and doesn't contain all the implementation - this class contains:
 * <ul>
 * <li>methods for generating an instance of {@link DefaultRepositorySystemSession} when necessary</li>
 * <li>methods for modification of properties related to the {@link DefaultRepositorySystemSession}</li>
 * <li>methods related to the {@link MavenWorkingSession} that doesn't need to have created an instance
 * of the {@link DefaultRepositorySystemSession} for its calling</li>
 * <li>delegating methods related to {@link Settings}</li>
 * </ul>
 *
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="mailto:mmatloka@gmail.com">Michal Matloka</a>
 */
public abstract class ConfigurableMavenWorkingSessionImpl implements MavenWorkingSession {

    private static final Logger log = Logger.getLogger(ConfigurableMavenWorkingSessionImpl.class.getName());

    private DefaultRepositorySystemSession session;
    private SettingsManager settingsManager;
    private boolean useLegacyLocalRepository = false;
    private final MavenRepositorySystem system;
    private boolean disableClassPathWorkspaceReader = false;

    public ConfigurableMavenWorkingSessionImpl() {
        this.system = new MavenRepositorySystem();
        this.settingsManager = new SettingsManager();
    }

    @Override
    public MavenWorkingSession configureSettingsFromFile(File globalSettings, File userSettings)
        throws InvalidConfigurationFileException {
        this.settingsManager.configureSettingsFromFile(globalSettings, userSettings);
        return regenerateSession();
    }

    @Override
    public MavenWorkingSession regenerateSession() {
        generateSession();
        return this;
    }

    @Override
    public void setOffline(final boolean offline) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("Set offline mode programatically to: " + offline);
        }
        this.settingsManager.setOffline(offline);

        // TODO: this won't be necessary when the deprecated API is removed
        regenerateSessionIfNotNull();
    }

    @Override
    public void disableClassPathWorkspaceReader() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Disabling ClassPath resolution");
        }
        disableClassPathWorkspaceReader = true;

        // TODO: this won't be necessary when the deprecated API is removed
        regenerateSessionIfNotNull();
    }

    @Override
    public void useLegacyLocalRepository(boolean useLegacyLocalRepository) {
        if (this.useLegacyLocalRepository == useLegacyLocalRepository) {
            return;
        }

        log.log(Level.FINEST, "Using legacy local repository");
        this.useLegacyLocalRepository = useLegacyLocalRepository;

        // TODO: this won't be necessary when the deprecated API is removed
        regenerateSessionIfNotNull();
    }

    /**
     * Returns an instance of the {@link DefaultRepositorySystemSession} that is generated if hasn't been yet.
     *
     * @return an instance of the {@link DefaultRepositorySystemSession}
     */
    protected DefaultRepositorySystemSession getSession() {
        if (this.session == null) {
            generateSession();
        }
        return this.session;
    }

    /**
     * Returns an instance of the {@link Settings}.
     *
     * @return an instance of the {@link Settings}
     * @see SettingsManager#getSettings()
     */
    protected Settings getSettings() {
        return this.settingsManager.getSettings();
    }

    /**
     * Returns whether the resolver should work in offline mode or not
     *
     * @return whether the resolver should work in offline mode or not
     * @see SettingsManager#isOffline()
     */
    protected boolean isOffline() {
        return this.settingsManager.isOffline();
    }

    /**
     * Returns an instance of the {@link MavenRepositorySystem}.
     *
     * @return an instance of the {@link MavenRepositorySystem}
     */
    protected MavenRepositorySystem getSystem() {
        return this.system;
    }

    // utility methods

    /**
     * Regenerates an instance of the {@link DefaultRepositorySystemSession} if there is already any
     */
    private void regenerateSessionIfNotNull() {
        if (this.session != null) {
            regenerateSession();
        }
    }

    /**
     * Generates an instance of the {@link DefaultRepositorySystemSession} and takes into account related properties
     */
    private void generateSession() {
        this.session = this.system.getSession(getSettings(), this.useLegacyLocalRepository);
        if (this.disableClassPathWorkspaceReader) {
            ((DefaultRepositorySystemSession) this.session).setWorkspaceReader(null);
        }
    }
}