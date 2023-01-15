/* LWJGL */
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.system.MemoryUtil.NULL;
/* Caliko j*/
import au.edu.federation.caliko.FabrikBone2D;
import au.edu.federation.caliko.FabrikChain2D;
import au.edu.federation.caliko.FabrikStructure2D;
import au.edu.federation.caliko.FabrikChain2D.BaseboneConstraintType2D;
import au.edu.federation.caliko.demo.OpenGLWindow;
import au.edu.federation.caliko.demo.CalikoDemo2D;
import au.edu.federation.caliko.visualisation.Point2D;
import au.edu.federation.caliko.visualisation.FabrikLine2D;
import au.edu.federation.utils.Utils;
import au.edu.federation.utils.Vec2f;
import au.edu.federation.utils.Mat4f;

public class InverseKinematicsVisualization {
   // We need to strongly reference callback instances.
   private GLFWErrorCallback errorCB;
   private GLFWKeyCallback keyCB;
   private GLFWMouseButtonCallback mouseButtonCallback;
   private GLFWCursorPosCallback cursorPosCallback;
   int WIDTH = 800;
   int HEIGHT = 600; // Window width and height
   private long window; // Window handle
   FabrikStructure2D structure = new FabrikStructure2D();
   FabrikChain2D chain = new FabrikChain2D(); // Create a new 2D chain
   // 2D projection matrix. Params: Left, Right, Top, Bottom, Near, Far
   Mat4f mvpMatrix = Mat4f.createOrthographicProjectionMatrix(
         -(float) WIDTH / 2.0f, (float) WIDTH / 2.0f,
         (float) HEIGHT / 2.0f, -(float) HEIGHT / 2.0f,
         1.0f, -1.0f);

   public void run() {
      // Create our chain
      float armLength = 258.19f;
      final Vec2f UP = new Vec2f(0.0f, 1.0f);
      final Vec2f RIGHT = new Vec2f(1.0f, 0.0f);
      float s1Ratio = (float) 8 / 16, s2Ratio = (float) 5 / 16, s3Ratio = (float) 3 / 16;
      float s1Length = armLength * s1Ratio, s2Length = armLength * s2Ratio, s3Length = armLength * s3Ratio;
      FabrikBone2D base = new FabrikBone2D(new Vec2f(), new Vec2f(0.0f, s1Length));
      base.setClockwiseConstraintDegs(45.0f);
      base.setAnticlockwiseConstraintDegs(45.0f);
      chain.addBone(base);
      float s2AngleConstraint = 135.0f;
      float s3AngleConstraint = 135.0f;
      chain.setBaseboneConstraintType(BaseboneConstraintType2D.GLOBAL_ABSOLUTE);
      chain.setBaseboneConstraintUV(UP);
      chain.addConsecutiveConstrainedBone(RIGHT, s2Length, s2AngleConstraint, s2AngleConstraint);
      chain.addConsecutiveConstrainedBone(new Vec2f(1.0f, -1.0f), s3Length, s3AngleConstraint, s3AngleConstraint);
      structure.addChain(chain);

      try {
         init();
         loop();
      } finally {
         // Free the keyboard callback and destroy the window
         keyCB.close();
         glfwDestroyWindow(window);

         // Terminate GLFW and free the error callback
         glfwTerminate();
         glfwSetErrorCallback(null).free();
      }
   }

   private void init() {
      // Setup an error callback. The default implementation
      // will print the error message in System.err.
      glfwSetErrorCallback(errorCB = GLFWErrorCallback.createPrint(System.err));
      // Initialize GLFW. Most GLFW functions will not work before doing this.
      if (!glfwInit())
         throw new IllegalStateException("Unable to initialize GLFW");
      glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
      // Create the window
      window = glfwCreateWindow(WIDTH, HEIGHT, "Hello, Caliko!", NULL, NULL);
      if (window == NULL)
         throw new RuntimeException("Failed to create the GLFW window");
      // Setup a key callback
      glfwSetKeyCallback(window, keyCB = new GLFWKeyCallback() {
         @Override
         public void invoke(long window, int key, int scancode, int action, int mods) {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
               glfwSetWindowShouldClose(window, true);
         }
      });
      // Get the resolution of the primary monitor
      GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

      // Center our window
      glfwSetWindowPos(window, (vidmode.width() - WIDTH) / 2,
            (vidmode.height() - HEIGHT) / 2);
      glfwMakeContextCurrent(window); // Make the OpenGL context current
      glfwSwapInterval(1); // Enable v-sync
      glfwShowWindow(window); // Make the window visible
   }

   static boolean leftMouseButtonDown = false;

   private void loop() {
      // This line is critical for LWJGL's interoperation with GLFW's
      // OpenGL context, or any context that is managed externally.
      // LWJGL detects the context that is current in the current thread,
      // creates the GLCapabilities instance and makes the OpenGL
      // bindings available for use.
      GL.createCapabilities();
      glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Set the clear color
      Point2D mTargetPoint = new Point2D();
      Vec2f screenMousePos = new Vec2f();
      Vec2f worldMousePos = new Vec2f();
      // Run the rendering loop until the user has attempted to close
      // the window or has pressed the ESCAPE key.
      while (glfwWindowShouldClose(window) == false) {
         glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear buffers

         glfwSetMouseButtonCallback(window, mouseButtonCallback = GLFWMouseButtonCallback
               .create((long windowId, int button, int action, int mods) -> {
                  if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                     InverseKinematicsVisualization.leftMouseButtonDown = true;
                  } else {
                     InverseKinematicsVisualization.leftMouseButtonDown = false;
                  }
                  glfwSetCursorPosCallback(window, cursorPosCallback = GLFWCursorPosCallback
                        .create((long windowId2, double mouseX, double mouseY) -> {
                           if (InverseKinematicsVisualization.leftMouseButtonDown == true) {
                              screenMousePos.set((float) mouseX, (float) mouseY);
                              worldMousePos.set(Utils.convertRange(screenMousePos.x, 0.0f, 800, -250.0f, 250.0f),
                                    -Utils.convertRange(screenMousePos.y, 0.0f, 600, -250.0f, 250.0f));
                              chain.solveForTarget(worldMousePos);
                              mTargetPoint.draw(new Vec2f(5.0f, 10.0f), Utils.YELLOW, 50.0f, mvpMatrix); // Solve the
                                                                                                         // chain
                           }
                        }));
               }));
         FabrikLine2D.draw(chain, 3.0f, mvpMatrix); // Draw the chain
         FabrikLine2D.drawChainConstraintAngles(chain, 20.0f, 2.0f, mvpMatrix);
         glfwSwapBuffers(window); // Swap colour buf.
         // Rotate the offset 1 degree per frame 
         // Poll for window events. The key callback above will only be
         // invoked during this call.
         glfwPollEvents();
      }
   }

   public static void main(String[] args) {
      new InverseKinematicsVisualization().run();
   }
}