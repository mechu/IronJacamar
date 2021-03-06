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

package org.jboss.jca.core.workmanager;

import org.jboss.jca.core.CoreBundle;
import org.jboss.jca.core.CoreLogger;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.spi.security.Callback;
import org.jboss.jca.core.spi.transaction.xa.XATerminator;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.HintsContext;
import javax.resource.spi.work.SecurityContext;
import javax.resource.spi.work.TransactionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkContextErrorCodes;
import javax.resource.spi.work.WorkContextLifecycleListener;
import javax.resource.spi.work.WorkContextProvider;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkRejectedException;

import org.jboss.logging.Logger;
import org.jboss.logging.Messages;
import org.jboss.threads.BlockingExecutor;
import org.jboss.threads.ExecutionTimedOutException;

/**
 * The work manager implementation.
 * 
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class WorkManagerImpl implements WorkManager
{
   /** The logger */
   private static CoreLogger log = Logger.getMessageLogger(CoreLogger.class, WorkManagerImpl.class.getName());
   
   /** Whether trace is enabled */
   private static boolean trace = log.isTraceEnabled();
   
   /** The bundle */
   private static CoreBundle bundle = Messages.getBundle(CoreBundle.class);
   
   /**Work run method name*/
   private static final String RUN_METHOD_NAME = "run";
   
   /**Work release method name*/
   private static final String RELEASE_METHOD_NAME = "release";

   /**Supported work context set*/
   private static final Set<Class<? extends WorkContext>> SUPPORTED_WORK_CONTEXT_CLASSES = 
       new HashSet<Class<? extends WorkContext>>(3); 

   /** Running in spec compliant mode */
   private boolean specCompliant;

   /** The short running executor */
   private BlockingExecutor shortRunningExecutor;

   /** The long running executor */
   private BlockingExecutor longRunningExecutor;

   /** The XA terminator */
   private XATerminator xaTerminator;

   /** Validated work instances */
   private Set<String> validatedWork;

   /** Security module for callback */
   private Callback callbackSecurity;

   /** Shutdown */
   private AtomicBoolean shutdown;

   /** Active work wrappers */
   private Set<WorkWrapper> activeWorkWrappers;

   /**Default supported workcontext types*/
   static
   {
      SUPPORTED_WORK_CONTEXT_CLASSES.add(TransactionContext.class);
      SUPPORTED_WORK_CONTEXT_CLASSES.add(SecurityContext.class);
      SUPPORTED_WORK_CONTEXT_CLASSES.add(HintsContext.class);
   }
   
   /**
    * Constructor - by default the WorkManager is running in spec-compliant mode
    */
   public WorkManagerImpl()
   {
      specCompliant = true;
      validatedWork = new HashSet<String>();
      shutdown = new AtomicBoolean(false);
      activeWorkWrappers = new HashSet<WorkWrapper>();
   }
   
   /**
    * Retrieve the executor for short running tasks
    * @return The executor
    */
   public BlockingExecutor getShortRunningThreadPool()
   {
      return shortRunningExecutor;
   }

   /**
    * Set the executor for short running tasks
    * @param executor The executor
    */
   public void setShortRunningThreadPool(BlockingExecutor executor)
   {
      this.shortRunningExecutor = executor;
   }

   /**
    * Retrieve the executor for long running tasks
    * @return The executor
    */
   public BlockingExecutor getLongRunningThreadPool()
   {
      return longRunningExecutor;
   }

   /**
    * Set the executor for long running tasks
    * @param executor The executor
    */
   public void setLongRunningThreadPool(BlockingExecutor executor)
   {
      this.longRunningExecutor = executor;
   }

   /**
    * Get the XATerminator
    * @return The XA terminator
    */
   public XATerminator getXATerminator()
   {
      return xaTerminator;
   }

   /**
    * Set the XATerminator
    * @param xaTerminator The XA terminator
    */
   public void setXATerminator(XATerminator xaTerminator)
   {
      this.xaTerminator = xaTerminator;
   }

   /**
    * Is spec compliant
    * @return True if spec compliant; otherwise false
    */
   public boolean isSpecCompliant()
   {
      return specCompliant;
   }

   /**
    * Set spec compliant flag
    * @param v The value
    */
   public void setSpecCompliant(boolean v)
   {
      specCompliant = v;
   }

   /**
    * Get the callback security module
    * @return The value
    */
   public Callback getCallbackSecurity()
   {
      return callbackSecurity;
   }

   /**
    * Set callback security module
    * @param v The value
    */
   public void setCallbackSecurity(Callback v)
   {
      callbackSecurity = v;
   }

   /**
    * Clone the WorkManager implementation
    * @return A copy of the implementation
    * @exception CloneNotSupportedException Thrown if the copy operation isn't supported
    *  
    */
   public WorkManager clone() throws CloneNotSupportedException
   {
      WorkManager wm = (WorkManager)super.clone();
      wm.setShortRunningThreadPool(getShortRunningThreadPool());
      wm.setLongRunningThreadPool(getLongRunningThreadPool());
      wm.setXATerminator(getXATerminator());
      wm.setSpecCompliant(isSpecCompliant());
      wm.setCallbackSecurity(getCallbackSecurity());
      
      return wm;
   }

   /**
    * {@inheritDoc}
    */
   public void doWork(Work work) throws WorkException
   {
      doWork(work, WorkManager.INDEFINITE, null, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public void doWork(Work work,
                      long startTimeout, 
                      ExecutionContext execContext, 
                      WorkListener workListener) 
      throws WorkException
   {
      if (trace)
         log.tracef("doWork(%s, %s, %s, %s)", work, startTimeout, execContext, workListener);

      if (shutdown.get())
         throw new WorkRejectedException(bundle.workmanagerShutdown());

      WorkException exception = null;
      WorkWrapper wrapper = null;
      try
      {
         if (work == null)
            throw new WorkRejectedException(bundle.workIsNull());

         if (startTimeout < 0)
            throw new WorkRejectedException(bundle.startTimeoutIsNegative(startTimeout));

         checkAndVerifyWork(work, execContext);
      
         if (execContext == null)
         {
            execContext = new ExecutionContext();  
         }

         final CountDownLatch completedLatch = new CountDownLatch(1);

         wrapper = new WorkWrapper(this, work, execContext, workListener, null, completedLatch);

         setup(wrapper);

         if (workListener != null)
         {
            WorkEvent event = new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null);
            workListener.workAccepted(event);
         }

         BlockingExecutor executor = getExecutor(work);

         if (startTimeout == WorkManager.INDEFINITE)
         {
            executor.executeBlocking(wrapper);
         }
         else
         {
            executor.executeBlocking(wrapper, startTimeout, TimeUnit.MILLISECONDS);
         }

         completedLatch.await();
      }
      catch (ExecutionTimedOutException etoe)
      {
         exception = new WorkRejectedException(etoe);
         exception.setErrorCode(WorkRejectedException.START_TIMED_OUT);  
      }
      catch (RejectedExecutionException ree)
      {
         exception = new WorkRejectedException(ree);
      }
      catch (WorkException we)
      {
         exception = we;
      }
      catch (InterruptedException ie)
      {
         Thread.currentThread().interrupt();
         exception = new WorkRejectedException(bundle.interruptedWhileRequestingPermit());
      }
      finally
      {
         if (exception != null)
         {
            if (workListener != null)
            {
               WorkEvent event = new WorkEvent(this, WorkEvent.WORK_REJECTED, work, exception);
               workListener.workRejected(event);
            }

            if (trace)
               log.tracef("Exception %s for %s", exception, this);

            throw exception;
         }

         if (wrapper != null)
         {
            checkWorkCompletionException(wrapper);
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public long startWork(Work work) throws WorkException
   {
      return startWork(work, WorkManager.INDEFINITE, null, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public long startWork(Work work, 
                         long startTimeout, 
                         ExecutionContext execContext, 
                         WorkListener workListener) 
      throws WorkException
   {
      log.tracef("startWork(%s, %s, %s, %s)", work, startTimeout, execContext, workListener);

      if (shutdown.get())
         throw new WorkRejectedException(bundle.workmanagerShutdown());

      WorkException exception = null;
      WorkWrapper wrapper = null;
      try
      {
         if (work == null)
            throw new WorkRejectedException(bundle.workIsNull());

         if (startTimeout < 0)
            throw new WorkRejectedException(bundle.startTimeoutIsNegative(startTimeout));

         long started = System.currentTimeMillis();

         checkAndVerifyWork(work, execContext);
      
         if (execContext == null)
         {
            execContext = new ExecutionContext();  
         }

         final CountDownLatch startedLatch = new CountDownLatch(1);

         wrapper = new WorkWrapper(this, work, execContext, workListener, startedLatch, null);

         setup(wrapper);

         if (workListener != null)
         {
            WorkEvent event = new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null);
            workListener.workAccepted(event);
         }

         BlockingExecutor executor = getExecutor(work);

         if (startTimeout == WorkManager.INDEFINITE)
         {
            executor.executeBlocking(wrapper);
         }
         else
         {
            executor.executeBlocking(wrapper, startTimeout, TimeUnit.MILLISECONDS);
         }

         startedLatch.await();

         return System.currentTimeMillis() - started;
      }
      catch (ExecutionTimedOutException etoe)
      {
         exception = new WorkRejectedException(etoe);
         exception.setErrorCode(WorkRejectedException.START_TIMED_OUT);  
      }
      catch (RejectedExecutionException ree)
      {
         exception = new WorkRejectedException(ree);
      }
      catch (WorkException we)
      {
         exception = we;
      }
      catch (InterruptedException ie)
      {
         Thread.currentThread().interrupt();
         exception = new WorkRejectedException(bundle.interruptedWhileRequestingPermit());
      }
      finally
      {
         if (exception != null)
         {
            if (workListener != null)
            {
               WorkEvent event = new WorkEvent(this, WorkEvent.WORK_REJECTED, work, exception);
               workListener.workRejected(event);
            }

            if (trace)
               log.tracef("Exception %s for %s", exception, this);

            throw exception;
         }

         if (wrapper != null)
            checkWorkCompletionException(wrapper);
      }

      return WorkManager.UNKNOWN;
   }
   
   /**
    * {@inheritDoc}
    */
   public void scheduleWork(Work work) throws WorkException
   {
      scheduleWork(work, WorkManager.INDEFINITE, null, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public void scheduleWork(Work work,
                            long startTimeout, 
                            ExecutionContext execContext, 
                            WorkListener workListener) 
      throws WorkException
   {
      log.tracef("scheduleWork(%s, %s, %s, %s)", work, startTimeout, execContext, workListener);

      if (shutdown.get())
         throw new WorkRejectedException(bundle.workmanagerShutdown());

      WorkException exception = null;
      WorkWrapper wrapper = null;
      try
      {
         if (work == null)
            throw new WorkRejectedException(bundle.workIsNull());

         if (startTimeout < 0)
            throw new WorkRejectedException(bundle.startTimeoutIsNegative(startTimeout));

         checkAndVerifyWork(work, execContext);
      
         if (execContext == null)
         {
            execContext = new ExecutionContext();  
         }

         wrapper = new WorkWrapper(this, work, execContext, workListener, null, null);

         setup(wrapper);

         if (workListener != null)
         {
            WorkEvent event = new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null);
            workListener.workAccepted(event);
         }

         BlockingExecutor executor = getExecutor(work);

         if (startTimeout == WorkManager.INDEFINITE)
         {
            executor.executeBlocking(wrapper);
         }
         else
         {
            executor.executeBlocking(wrapper, startTimeout, TimeUnit.MILLISECONDS);
         }
      }
      catch (ExecutionTimedOutException etoe)
      {
         exception = new WorkRejectedException(etoe);
         exception.setErrorCode(WorkRejectedException.START_TIMED_OUT);  
      }
      catch (RejectedExecutionException ree)
      {
         exception = new WorkRejectedException(ree);
      }
      catch (WorkException we)
      {
         exception = we;
      }
      catch (InterruptedException ie)
      {
         Thread.currentThread().interrupt();
         exception = new WorkRejectedException(bundle.interruptedWhileRequestingPermit());
      }
      finally
      {
         if (exception != null)
         {
            if (workListener != null)
            {
               WorkEvent event = new WorkEvent(this, WorkEvent.WORK_REJECTED, work, exception);
               workListener.workRejected(event);
            }

            if (trace)
               log.tracef("Exception %s for %s", exception, this);

            throw exception;
         }

         if (wrapper != null)
            checkWorkCompletionException(wrapper);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void prepareShutdown()
   {
      shutdown.set(true);
   }

   /**
    * {@inheritDoc}
    */
   public void shutdown()
   {
      prepareShutdown();

      synchronized (activeWorkWrappers)
      {
         for (WorkWrapper ww : activeWorkWrappers)
         {
            ww.getWork().release();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isShutdown()
   {
      return shutdown.get();
   }

   /**
    * Add work wrapper to active set
    * @param ww The work wrapper
    */
   void addWorkWrapper(WorkWrapper ww)
   {
      synchronized (activeWorkWrappers)
      {
         activeWorkWrappers.add(ww);
      }
   }

   /**
    * Remove work wrapper from active set
    * @param ww The work wrapper
    */
   void removeWorkWrapper(WorkWrapper ww)
   {
      synchronized (activeWorkWrappers)
      {
         activeWorkWrappers.remove(ww);
      }
   }

   /**
    * Get the executor
    * @param work The work instance
    * @return The executor
    */
   private BlockingExecutor getExecutor(Work work)
   {
      BlockingExecutor executor = shortRunningExecutor;
      if (work instanceof WorkContextProvider)
      {
         WorkContextProvider wcProvider = (WorkContextProvider)work;
         List<WorkContext> contexts = wcProvider.getWorkContexts();

         if (contexts != null && contexts.size() > 0)
         {
            boolean found = false;
            Iterator<WorkContext> it = contexts.iterator();
            while (!found && it.hasNext())
            {
               WorkContext wc = it.next();
               if (wc instanceof HintsContext)
               {
                  HintsContext hc = (HintsContext)wc;
                  if (hc.getHints().containsKey(HintsContext.LONGRUNNING_HINT))
                  {
                     executor = longRunningExecutor;
                     found = true;
                  }
               }
            }
         }
      }

      return executor;
   }



   /**
    * Check and verify work before submitting.
    * @param work the work instance
    * @param executionContext any execution context that is passed by apadater
    * @throws WorkException if any exception occurs
    */
   private void checkAndVerifyWork(Work work, ExecutionContext executionContext) throws WorkException
   {
      if (specCompliant)
      {
         verifyWork(work);  
      }   
      
      if (work instanceof WorkContextProvider)
      {
          //Implements WorkContextProvider and not-null ExecutionContext
         if (executionContext != null)
         {
            throw new WorkRejectedException(bundle.workExecutionContextMustNullImplementsWorkContextProvider());
         }          
      }      
   }
   
   /**
    * Verify the given work instance.
    * @param work The work
    * @throws WorkException Thrown if a spec compliant issue is found
    */
   private void verifyWork(Work work) throws WorkException
   {     
      if (!validatedWork.contains(work.getClass().getName()))
      {
         Class<?> workClass = work.getClass();
         boolean result = false;
      
         result = verifyWorkMethods(workClass, RUN_METHOD_NAME, null, workClass.getName() + 
                                    ": Run method is not defined");
     
         if (!result)
         {
            throw new WorkException(bundle.runMethodIsSynchronized(workClass.getName()));
         }
      
         result = verifyWorkMethods(workClass, RELEASE_METHOD_NAME, null, workClass.getName() + 
                                    ": Release method is not defined");
      
         if (!result)
         {
            throw new WorkException(bundle.releaseMethodIsSynchronized(workClass.getName()));
         }

         validatedWork.add(work.getClass().getName());
      }
   }
   
   private boolean verifyWorkMethods(Class<?> workClass, String methodName, 
                                     Class<?>[] parameterTypes, String errorMessage) throws WorkException
   {
      Method method = null;
      try
      {
         method = ClassUtil.getClassMethod(workClass, methodName, null);
         
         if (ClassUtil.modifiersHasSynchronizedKeyword(method.getModifiers()))
         {
            return false;  
         }
      }
      catch (NoSuchMethodException nsme)
      {
         throw new WorkException(errorMessage);
      }
      
      return true;
   }

   /**
    * Checks work completed status. 
    * @param wrapper work wrapper instance
    * @throws {@link WorkException} if work is completed with an exception
    */
   private void checkWorkCompletionException(WorkWrapper wrapper) throws WorkException
   {
      if (wrapper.getWorkException() != null)
      {
         if (trace)
            log.tracef("Exception %s for %s", wrapper.getWorkException(), this);

         throw wrapper.getWorkException();  
      }
   }

   /**
    * Setup work context's of the given work instance.
    * 
    * @param wrapper The work wrapper instance
    * @throws WorkException if any exception occurs
    */
   private void setup(WorkWrapper wrapper) throws WorkException
   {
      if (trace)
      {
         log.trace("Setting up work contexts " + wrapper);  
      }

      Work work = wrapper.getWork();
      
      //If work is an instanceof WorkContextProvider
      if (work instanceof WorkContextProvider)
      {
         WorkContextProvider wcProvider = (WorkContextProvider)work;
         List<WorkContext> contexts = wcProvider.getWorkContexts();

         if (contexts != null && contexts.size() > 0)
         {
            boolean isTransactionContext = false;
            boolean isSecurityContext = false;
            boolean isHintcontext = false;

            for (WorkContext context : contexts)
            {
               Class<? extends WorkContext> contextType = null;

               // Get supported work context class
               contextType = getSupportedWorkContextClass(context.getClass());

               // Not supported
               if (contextType == null)
               {
                  if (trace)
                  {
                     log.trace("Not supported work context class : " + context.getClass().getName());
                  }
                  
                  fireWorkContextSetupFailed(context, WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE);
                  
                  throw new WorkCompletedException(bundle.unsupportedWorkContextClass(context.getClass().getName()), 
                      WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE);
               }
               // Duplicate checks
               else
               {
                  // TransactionContext duplicate
                  if (isTransactionContext(contextType))
                  {
                     if (isTransactionContext)
                     {
                        if (trace)
                        {
                           log.trace("Duplicate transaction work context : " + context.getClass().getName());
                        }

                        fireWorkContextSetupFailed(context, WorkContextErrorCodes.DUPLICATE_CONTEXTS);
                        
                        throw new WorkCompletedException(bundle.duplicateTransactionWorkContextClass(
                              context.getClass().getName()), WorkContextErrorCodes.DUPLICATE_CONTEXTS);
                     }
                     else
                     {
                        isTransactionContext = true;
                     }
                  }
                  // SecurityContext duplicate
                  else if (isSecurityContext(contextType))
                  {
                     if (isSecurityContext)
                     {
                        if (trace)
                        {
                           log.trace("Duplicate security work context : " + context.getClass().getName());
                        }
                        
                        fireWorkContextSetupFailed(context, WorkContextErrorCodes.DUPLICATE_CONTEXTS);

                        throw new WorkCompletedException(bundle.duplicateSecurityWorkContextClass(
                              context.getClass().getName()), WorkContextErrorCodes.DUPLICATE_CONTEXTS);
                     }
                     else
                     {
                        isSecurityContext = true;
                     }
                  }
                  // HintContext duplicate
                  else if (isHintContext(contextType))
                  {
                     if (isHintcontext)
                     {
                        if (trace)
                        {
                           log.trace("Duplicate hint work context : " + context.getClass().getName());
                        }

                        fireWorkContextSetupFailed(context, WorkContextErrorCodes.DUPLICATE_CONTEXTS);
                        
                        throw new WorkCompletedException(bundle.duplicateHintWorkContextClass(
                              context.getClass().getName()), WorkContextErrorCodes.DUPLICATE_CONTEXTS);
                     }
                     else
                     {
                        isHintcontext = true;
                     }
                  }
                  // Normally, this must not be happened!i just safe check!
                  else
                  {
                     fireWorkContextSetupFailed(context, WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE);
                     
                     throw new WorkCompletedException(bundle.unsupportedWorkContextClass(context.getClass().getName()), 
                                                      WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE);
                  }
               }

               // Add workcontext instance to the work
               wrapper.addWorkContext(contextType, context);
            }
         }         
      }      

      if (trace)
      {
         log.trace("Setted up work contexts " + wrapper);  
      }      
   }

   /**
    * Calls listener with given error code.
    * @param listener work context listener
    * @param errorCode error code
    */
   private void fireWorkContextSetupFailed(Object workContext, String errorCode)
   {
      if (workContext instanceof WorkContextLifecycleListener)
      {
         WorkContextLifecycleListener listener = (WorkContextLifecycleListener)workContext;
         listener.contextSetupFailed(errorCode);   
      }
      
   }

   /**
    * Returns true if contexts is a transaction context.
    * 
    * @param workContextType context type
    * @return true if contexts is a transaction context
    */
   private boolean isTransactionContext(Class<? extends WorkContext> workContextType)
   {
      if (workContextType.isAssignableFrom(TransactionContext.class))
      {
         return true;
      }

      return false;
   }

   /**
    * Returns true if contexts is a security context.
    * 
    * @param workContextType context type
    * @return true if contexts is a security context
    */
   private boolean isSecurityContext(Class<? extends WorkContext> workContextType)
   {
      if (workContextType.isAssignableFrom(SecurityContext.class))
      {
         return true;
      }

      return false;
   }

   /**
    * Returns true if contexts is a hint context.
    * 
    * @param workContextType context type
    * @return true if contexts is a hint context
    */
   private boolean isHintContext(Class<? extends WorkContext> workContextType)
   {
      if (workContextType.isAssignableFrom(HintsContext.class))
      {
         return true;
      }

      return false;
   }

   /**
    * Returns work context class if given work context is supported by server,
    * returns null instance otherwise.
    * 
    * @param <T> work context class
    * @param adaptorWorkContext adaptor supplied work context class
    * @return work context class
    */
   @SuppressWarnings("unchecked")
   private <T extends WorkContext> Class<T> getSupportedWorkContextClass(Class<T> adaptorWorkContext)
   {
      for (Class<? extends WorkContext> supportedWorkContext : SUPPORTED_WORK_CONTEXT_CLASSES)
      {
         // Assignable or not
         if (supportedWorkContext.isAssignableFrom(adaptorWorkContext))
         {
            Class clz = adaptorWorkContext;

            while (clz != null)
            {
               // Supported by the server
               if (clz.equals(supportedWorkContext))
               {
                  return clz;
               }

               clz = clz.getSuperclass();
            }
         }
      }

      return null;
   }
}
