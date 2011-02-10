/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.jca.samples.helloworld;

import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;


/**
 * ConnectorTestCase
 *
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class ConnectorTestCase
{
   private static Logger log = Logger.getLogger("ConnectorTestCase");

   private static String deploymentName = "ConnectorTestCase";

   /**
    * Define the deployment
    *
    * @return The deployment archive
    */
   @Deployment
   public static ResourceAdapterArchive createDeployment()
   {
      ResourceAdapterArchive raa =
         ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName + ".rar");
      JavaArchive ja = ShrinkWrap.create(JavaArchive.class, 
         UUID.randomUUID().toString() + ".jar");
      ja.addClasses(HelloWorldResourceAdapter.class, 
         HelloWorldManagedConnectionFactory.class, 
         HelloWorldManagedConnection.class, 
         HelloWorldManagedConnectionMetaData.class, 
         HelloWorldConnectionFactory.class, 
         HelloWorldConnectionFactoryImpl.class, 
         HelloWorldConnection.class, 
         HelloWorldConnectionImpl.class);
      raa.addLibrary(ja);

      return raa;
   }

   /** resource */
   @Resource(mappedName = "java:/eis/ConnectorTestCase")
   private HelloWorldConnectionFactory connectionFactory;

   /**
    * Test helloWorld
    *
    * @exception Throwable Thrown if case of an error
    */
   @Test
   public void testHelloWorldNoArgs() throws Throwable
   {
      assertNotNull(connectionFactory);
      HelloWorldConnection connection = connectionFactory.getConnection();
      assertNotNull(connection);
      String result = connection.helloWorld();
   }

   /**
    * Test helloWorld
    *
    * @exception Throwable Thrown if case of an error
    */
   @Test
   public void testHelloWorldNameString() throws Throwable
   {
      assertNotNull(connectionFactory);
      HelloWorldConnection connection = connectionFactory.getConnection();
      assertNotNull(connection);
      String result = connection.helloWorld(null);
   }


}
