/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.jca.test.core.spec.chapter11.api;

import org.jboss.jca.embedded.EmbeddedJCA;
import org.jboss.jca.test.core.spec.chapter11.section4.subsection3.WorkContextHandlingAssignmentTestCase;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;



/**
 * WorkContextProviderGetWorkContextsTestCase.
 * @version $Rev$ $Date$
 *
 */
public class WorkContextProviderGetWorkContextsTestCase
{
   /*
    * Embedded
    */
   private static EmbeddedJCA embedded;
   
   /**
    * Test api for {@link WorkContextProvider#getWorkContexts()}
    */
   @Test
   public void testGetWorkContextsNumber()
   {
      org.jboss.jca.test.core.spec.chapter11.common.TransactionContextWork work = 
             new org.jboss.jca.test.core.spec.chapter11.common.TransactionContextWork();
      Assert.assertNotNull(work.getWorkContexts());

      Assert.assertEquals(1, work.getWorkContexts().size());
   }

   /**
    * Before class.
    * @throws Throwable throwable exception 
    */
   @BeforeClass
   public static void beforeClass() throws Throwable
   {
      // Create and set an embedded JCA instance
      embedded = new EmbeddedJCA(false);

      // Startup
      embedded.startup();

      // Deploy Naming, Transaction and WorkManager
      embedded.deploy(WorkContextHandlingAssignmentTestCase.class.getClassLoader(), "naming-jboss-beans.xml");
      embedded.deploy(WorkContextHandlingAssignmentTestCase.class.getClassLoader(), "transaction-jboss-beans.xml");
      embedded.deploy(WorkContextHandlingAssignmentTestCase.class.getClassLoader(), "workmanager-jboss-beans.xml");

   }

   /**
    * After class.
    * @throws Throwable throwable exception 
    */
   @AfterClass
   public static void afterClass() throws Throwable
   {
      embedded.undeploy(WorkContextHandlingAssignmentTestCase.class.getClassLoader(), "workmanager-jboss-beans.xml");
      embedded.shutdown();

      embedded = null;
   }

}
