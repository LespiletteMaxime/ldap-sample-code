/*
 * Copyright 2008-2011 UnboundID Corp. All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2011 UnboundID Corp. This program is free
 * software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License (GPLv2 only) or the terms of the GNU
 * Lesser General Public License (LGPLv2.1 only) as published by the
 * Free Software Foundation. This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along
 * with this program; if not, see <http://www.gnu.org/licenses>.
 */

package samplecode.auth;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.args.Argument;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import samplecode.annotation.Author;
import samplecode.annotation.CodeVersion;
import samplecode.annotation.Launchable;
import samplecode.annotation.Since;
import samplecode.listener.DefaultLdapExceptionListener;
import samplecode.listener.LdapExceptionListener;
import samplecode.tools.AbstractTool;
import samplecode.tools.BasicToolCompletedProcessing;
import samplecode.tools.ToolCompletedProcessing;
import samplecode.util.SampleCodeCollectionUtils;

import java.util.Collection;


/**
 * Provides a demonstration of the Who Am I? extended operation and the
 * {@code AuthorizationIdentityRequestControl}.
 * <p/>
 * Example usage:
 * <p/>
 * <pre>
 * java -cp your-classpath samplecode.auth.AuthDemo \
 *   --hostname localhost --port 1389 \
 *   --bindDn "uid=user.0,ou=people,dc=example,dc=com" \
 *   --bindPasswordFile ~/.pwdFile --useStartTLS --trustAll
 *
 * [18/Dec/2011:19:47:34 -0500] Connected to LDAP server.
 * [18/Dec/2011:19:47:34 -0500] Who Am I? extension is supported.
 * [18/Dec/2011:19:47:34 -0500] Authorization Identity Request Control is
 * supported.
 * [18/Dec/2011:19:47:34 -0500] AuthorizationID from the Who am I? extended
 * request:
 * 'dn:uid=user.0,ou=People,dc=example,dc=com'
 * [18/Dec/2011:19:47:34 -0500] AuthorizationID from the
 * AuthorizationIdentityResponseControl: 'dn:uid=user.0,ou=People,dc=example,dc=com'
 * [18/Dec/2011:19:47:34 -0500] PasswordExpiredControl was not included in the
 * bind
 * response.
 * [18/Dec/2011:19:47:34 -0500] PasswordExpiringControl was not included in the
 * bind
 * response.
 * [18/Dec/2011:19:47:34 -0500] AuthDemo has completed processing. The result
 * code was: 0
 * (success)
 * </pre>
 * <p/>
 * Below is the output of the {@code --help|-H} command line
 * option:<blockquote>
 * <p/>
 * <pre>
 *
 * </pre>
 * <p/>
 * </blockquote>
 */
@Author("terry.gardner@unboundid.com")
@Since("27-Nov-2011")
@CodeVersion("2.5")
@Launchable
public final class AuthDemo extends AbstractTool {

  /**
   * Launch the {@code AuthDemo} application.
   *
   * @param args
   *   command line arguments, less the JVM arguments.
   */
  public static void main(final String... args) {
    final AuthDemo authDemo = new AuthDemo();
    final ResultCode resultCode = authDemo.runTool(args);
    final ToolCompletedProcessing completedProcessing =
      new BasicToolCompletedProcessing(authDemo,resultCode);
    final Log logger = LogFactory.getLog(AuthDemo.class);
    completedProcessing.displayMessage(logger);
    if(!resultCode.equals(ResultCode.SUCCESS)) {
      System.exit(resultCode.intValue());
    }
  }



  /**
   * Adds {@code --bindDN} to the required argument set
   */
  @Override
  protected void addArguments(final ArgumentParser argumentParser)
    throws ArgumentException {
    Argument requiredArgument =
      commandLineOptions.getBindDnArgument();

    final Collection<Argument> requiredArgumentSet =
      SampleCodeCollectionUtils.newArrayList();
    requiredArgumentSet.add(requiredArgument);
    requiredArgument = commandLineOptions.getBindPasswordArgument();
    requiredArgumentSet.add(requiredArgument);
    argumentParser.addRequiredArgumentSet(requiredArgumentSet);
  }



  @Override
  protected ResultCode executeToolTasks() {
    final Log logger = getLogger();
    final LdapExceptionListener l = new DefaultLdapExceptionListener(logger);
    addLdapExceptionListener(l);

    /*
     * Obtain a pool of connections to the LDAP server from the
     * LDAPCommandLineTool services,this requires specifying a
     * connection to the LDAP server,a number of initial connections
     * (--initialConnections) in the pool,and the maximum number of
     * connections (--maxConnections) that the pool should create.
     */
    try {
      ldapConnection = connectToServer();
      ldapConnectionPool = getLdapConnectionPool(ldapConnection);
    } catch(final LDAPException x) {
      fireLdapExceptionListener(ldapConnection,x);
      return x.getResultCode();
    }

    /*
     * Instantiate the object which provides methods to get the
     * authorization identity. Add an instance of the default
     * LDAP Exception Listener to the authorized identity provider.
     */
    if(isVerbose()) {
      verbose("Creating the authorized identity object.");
    }
    final AuthorizedIdentity authorizedIdentity =
      new AuthorizedIdentity(ldapConnection);
    final LdapExceptionListener listener =
      new DefaultLdapExceptionListener(getLogger());
    authorizedIdentity.addLdapExceptionListener(listener);

    /*
     * String representation of messages that provide informative or
     * instructional messages.
     */
    String msg;

    /*
     * Demonstrate the user of the Who Am I? extended operation. This
     * procedure requires creating a WhoAmIExtendedRequest object and
     * using processExtendedOperation to transmit it.
     */
    if(isVerbose()) {
      verbose("Getting the authorization identity using the Who Am I? " +
        "extended request.");
    }
    String authId;
    authId = authorizedIdentity
      .getAuthorizationIdentityWhoAmIExtendedOperation();
    if(authId != null) {
      msg = String.format("AuthorizationID from the Who am I? extended request: " +
        "'%s'",authId);
      getLogger().info(msg);
    }

    /*
     * Demonstrate the use of the AuthorizationIdentityRequestControl.
     */
    final DN bindDnAsDn = commandLineOptions.getBindDn();
    if(isVerbose()) {
      verbose("Getting the authorization identity using the authorization identity " +
        "request.");
    }
    final String bindDn = bindDnAsDn.toString();
    final String bindPassword = commandLineOptions.getBindPassword();
    final long responseTimeMillis = getResponseTimeMillis();
    authId =
      authorizedIdentity.getAuthorizationIdentityFromBindRequest(bindDn,
        bindPassword,responseTimeMillis);
    if(authId != null) {
      msg =
        String.format("AuthorizationID from the " +
          "AuthorizationIdentityResponseControl: '%s'",authId);
      getLogger().info(msg);
    }

    /**
     * Demonstration is complete, close the connection(s)
     */
    ldapConnection.close();
    return ResultCode.SUCCESS;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected String classSpecificPropertiesResourceName() {
    return "AuthDemo.properties";
  }

}
