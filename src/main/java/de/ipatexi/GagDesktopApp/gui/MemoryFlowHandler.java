package de.ipatexi.GagDesktopApp.gui;

import java.util.HashMap;

import io.datafx.controller.ViewConfiguration;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.action.FlowLink;
import io.datafx.controller.flow.context.ViewFlowContext;
import io.datafx.controller.flow.event.AfterFlowActionEvent;
import io.datafx.controller.flow.event.AfterFlowActionHandler;
import io.datafx.controller.util.VetoException;
import io.datafx.core.ExceptionHandler;
import javafx.beans.property.SimpleObjectProperty;

/**
 * TODO doesn't work the way I expected it to work. ...
 * @author Kilian
 *
 */
public class MemoryFlowHandler extends FlowHandler{

	String currentActionId;
	
	//Connect an actionId with a controller class. If we see the same actionId again we can jump back
	//To the correct flow.
	HashMap<String,Class> controllerClassActionMap = new HashMap<>();
	
	
	public MemoryFlowHandler(Flow flow, ViewFlowContext flowContext) {
		super(flow, flowContext);
		init();
	}
	
	public MemoryFlowHandler(Flow flow, ViewFlowContext flowContext, ViewConfiguration viewConfiguration) {
		super(flow, flowContext, viewConfiguration);
		init();
	}
	
	public MemoryFlowHandler(Flow flow, ViewFlowContext flowContext, ViewConfiguration viewConfiguration,
			ExceptionHandler exceptionHandler) {
		super(flow, flowContext, viewConfiguration, exceptionHandler);
		init();
	}
	
	
	@Override
	public void handle(String id) throws VetoException, FlowException {
		//Don't do anything if we currently are already on this is
		System.out.println("Cur:" + currentActionId + " " + this);
		if(!id.equals(currentActionId)) {	
			if(controllerClassActionMap.containsKey(id)) {
				System.out.println("Exists");
				this.navigateTo(controllerClassActionMap.get(id));
			}else {
				super.handle(id);
				System.out.println("Does not exist");
			}
		}
		
	}
	
	
	private void init() {
		System.out.println(this);
		
		try {
			java.lang.reflect.Field f = FlowHandler.class.getDeclaredField("afterFlowActionHandler");
			f.setAccessible(true);
			
			//SimpleObjectProperty<AfterFlowActionHandler> afterFlowActionHandler = (SimpleObjectProperty<AfterFlowActionHandler>) f.get(this);
			
			SimpleObjectProperty<AfterFlowActionHandler> afterFlowActionHandler = new SimpleObjectProperty<AfterFlowActionHandler>(new AfterFlowActionHandler() {

				@Override
				public void handle(AfterFlowActionEvent event) {
					System.out.println(event.getActionId() + " " + event.getEventType().getName()
							+ event.getSource() + " " + event.getTarget() + " " + event.getAction());
					currentActionId = event.getActionId();
					
					FlowLink link = (FlowLink) event.getAction();
					
					try {
						java.lang.reflect.Field flowClass = FlowLink.class.getDeclaredField("controllerClass");
					
						flowClass.setAccessible(true);
							
					
						controllerClassActionMap.put(event.getActionId(), (Class)flowClass.get(link));
						
						flowClass.setAccessible(false);
						
						
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
					
					
					
					System.out.println(link);
					//Somehow get the current flow...
					//event.getFlowContext();
				}
				
			});
			
			f.set(this,afterFlowActionHandler);
			
			f.setAccessible(false);
			
			
			
			
			
			System.out.println(afterFlowActionHandler);
			
			
			
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	

	

}
