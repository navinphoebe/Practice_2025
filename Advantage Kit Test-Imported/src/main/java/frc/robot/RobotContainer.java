// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import frc.robot.Constants.OperatorConstants;
import frc.robot.commands.ShooterDefaultCommand;
import frc.robot.commands.ArmDefaultCommand;
import frc.robot.commands.Autos;
import frc.robot.commands.DrivetrainDefaultCommand;
import frc.robot.commands.ExampleCommand;
import frc.robot.commands.MoveArmAndShooterToPosition;
import frc.robot.commands.MoveArmRelativeDegrees;
import frc.robot.commands.MoveShooterRelativeDegrees;
import frc.robot.commands.MoveShooterToPosition;
import frc.robot.commands.TurnToAutoAlign;
import frc.robot.subsystems.ArmSubsystem;
import frc.robot.subsystems.DriveIO;
import frc.robot.subsystems.DriveSimIO;
import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.DrivetrainSubsystemSim;
import frc.robot.subsystems.ExampleSubsystem;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.util.NoteVisualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.littletonrobotics.junction.Logger;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.path.PathPlannerPath;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

/**
 * This class is where the bulk of the robot should be declared. Since
 * Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in
 * the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of
 * the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...
  private final ExampleSubsystem m_exampleSubsystem = new ExampleSubsystem();
  // Replace with CommandPS4Controller or CommandJoystick if needed
  public final CommandXboxController m_driverController = new CommandXboxController(
      OperatorConstants.kDriverControllerPort);

  public Drivetrain m_drivetrain = new DrivetrainSubsystemSim(new DriveSimIO());
  public ShooterSubsystem m_shooter = new ShooterSubsystem();
  public ArmSubsystem m_arm = new ArmSubsystem(m_shooter);
  // public FlywheelSubystem m_flywheel = new FlywheelAkSubsystem(new FlywheelAKIOFSSim());
  // public final DrivetrainSwerveDrive m_swerveDrive = new
  // DrivetrainSwerveDrive();
  public Vision m_vision;

  SendableChooser<Command> m_chooser = new SendableChooser<>();
  SendableChooser<Command> m_redChooser = new SendableChooser<>();
  SendableChooser<Command> m_blueChooser = new SendableChooser<>();
  SendableChooser<Command> m_positionChooser = m_redChooser;
  Stream<Translation2d> presentNotes;
  private ArrayList<Pose3d> blueNotesPositions = new ArrayList<Pose3d>();
  public double[] speakerAuto = {2.458, 4.119, -22.001579};
  public double[] ampAuto = {1.85, 7.78, -90};
  public double[] autoPosition = speakerAuto;
  public int xx;

  public static DrivetrainState DRIVETRAIN_STATE = DrivetrainState.FREEHAND;

  public static ArmState ARM_STATE = ArmState.STAGE_ALIGN;

  public enum DrivetrainState {
    FREEHAND,
    ROBOT_ALIGN,
  }

  public enum ArmState {
    AMP_STATE,
    STAGE_ALIGN,
  }

  public static PickupState PICKUP_STATE = PickupState.NO_NOTE;

  public enum PickupState {
    NO_NOTE,
    HAS_NOTE
  }

  /**
   * The container for the robot. Contains subsystems, OI devices, and commands.
   */
  public RobotContainer() {
    
    
    NamedCommands.registerCommand("Print Command", m_arm.printCommand());
    NamedCommands.registerCommand("Shoot Command", getShooterCommand());
    NamedCommands.registerCommand("Pickup Note Command", new InstantCommand(() -> getPickUpNote()));
    NamedCommands.registerCommand("Pickup Position Command", new MoveShooterToPosition(m_shooter, 50));
    NamedCommands.registerCommand("Shooter Position 1", new MoveShooterToPosition(m_shooter, 43));
      
    // Configure the trigger bindings
    m_vision = new Vision(m_drivetrain);
    // NoteVisualizer.setRobotPoseSupplier(this::getPose);
    DRIVETRAIN_STATE = DrivetrainState.FREEHAND;
    NoteVisualizer.setPoseSuppliers(this::getPose, this::getAngle2, this::getPoseB);
    NoteVisualizer.resetAutoNotes();
    NoteVisualizer.showAutoNotes();
    presentNotes = NoteVisualizer.autoNotes.stream().filter(Objects::nonNull);
    Pose3d[] blueNotesPositionsArray = presentNotes
      .map(
          translation -> new Pose3d(
              translation.getX(),
              translation.getY(),
              Units.inchesToMeters(1.0),
              new Rotation3d()))
      .toArray(Pose3d[]::new);
    for (int i = 0; i < blueNotesPositionsArray.length; i++) {
      blueNotesPositions.add(blueNotesPositionsArray[i]);
    }
    m_shooter.addVision(m_vision);
    
  
    configureBindings();

  }

  public Pose2d getPose() {
    return m_drivetrain.getPose();
  }

  public Pose3d getPoseB() {
    return m_arm.getPoseB();
  }

  public Rotation2d getAngle2() {
    return new Rotation2d(Math.toRadians(m_shooter.getAngle2()));
  }

  /**
   * Use this method to define your trigger->command mappings. Triggers can be
   * created via the
   * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with
   * an arbitrary
   * predicate, or via the named factories in {@link
   * edu.wpi.first.wpilibj2.command.button.CommandGenericHID}'s subclasses for
   * {@link
   * CommandXboxController
   * Xbox}/{@link edu.wpi.first.wpilibj2.command.button.CommandPS4Controller
   * PS4} controllers or
   * {@link edu.wpi.first.wpilibj2.command.button.CommandJoystick Flight
   * joysticks}.
   */
  private void configureBindings() {
    // Schedule `ExampleCommand` when `exampleCondition` changes to `true`
    new Trigger(m_exampleSubsystem::exampleCondition)
        .onTrue(new ExampleCommand(m_exampleSubsystem));

    // Schedule `exampleMethodCommand` when the Xbox controller's B button is
    // pressed,
    // cancelling on release.
    m_drivetrain.setDefaultCommand(getSwerveDriveCommand());

    m_chooser.setDefaultOption("Follow Path", getAutonomousCommand());
    //m_chooser.addOption("On the Fly", getOnTheFlyPathComamand());
    //m_chooser.addOption("Navagation Grid", getNavigationGridDemoPathCommand());
    SmartDashboard.putData(m_chooser);
    //m_driverController.pov(0).whileTrue(new MoveArmRelativeDegrees(m_arm, -1.0));
    //m_driverController.pov(90).whileTrue(new MoveArmRelativeDegrees(m_arm, 1.0));
    //m_driverController.pov(180).whileTrue((new MoveArmAndShooterToPosition(m_shooter, m_arm, -45, 70)));
    //m_driverController.pov(270).whileTrue((new MoveArmAndShooterToPosition(m_shooter, m_arm, 5, 50))
       // .alongWith(new InstantCommand(() -> DRIVETRAIN_STATE = DrivetrainState.FREEHAND)));

    // m_driverController..onTrue(new SetMotorSpeed(m_flywheel, 5)).onFalse(new
    // SetMotorSpeed(m_flywheel, 0));
    m_shooter.setDefaultCommand((new ShooterDefaultCommand(m_vision, m_shooter)));
    m_arm.setDefaultCommand(new ArmDefaultCommand(m_arm));
    // m_driverController.x().whileTrue((new TurnToAutoAlign(m_drivetrain,
    // m_vision)));
    //m_driverController.x().whileTrue(goToAutoTargetPosition());
    m_driverController.y().whileTrue(new InstantCommand(() -> getPickUpNote()));
    m_driverController.a().onTrue(getShooterCommand());
    m_driverController.b().whileTrue((new InstantCommand(() -> toggleSpeakerandAmp()))); 


    m_driverController.x().onTrue((new InstantCommand(() -> DRIVETRAIN_STATE = DrivetrainState.ROBOT_ALIGN))
       .alongWith((new MoveArmAndShooterToPosition(m_shooter, m_arm, 5))));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */

  public void toggleSpeakerandAmp() {
    System.out.println("start of method");
    if (ArmState.AMP_STATE == ARM_STATE) {
      System.out.println("first change method");
      ARM_STATE = ArmState.STAGE_ALIGN;
    } else {
    System.out.println("second change method");
    ARM_STATE = ArmState.AMP_STATE;
    }
    
  }

  public void getPickUpNote() {
    for (int i = 0; i < blueNotesPositions.size(); i++) {
      Logger.recordOutput("point" + i, blueNotesPositions.get(i));
    }
    int num = m_drivetrain.pointInBox(blueNotesPositions);
    if (num != -1) {
      NoteVisualizer.takeAutoNote(num);
      NoteVisualizer.showAutoNotes();
      NoteVisualizer.showHeldNotes();
    }

  }

  public void setChooserToBlue() {
    m_positionChooser = m_blueChooser;
  }

  public Command getShooterCommand() {
    return NoteVisualizer.shoot();
  }

  public Command getEjectCommand() {
    return NoteVisualizer.eject();
  }

  public void setChooserToRed() {
    m_positionChooser = m_redChooser;
  }

  public Command getTargetPositionCommand() {
    return m_positionChooser.getSelected();
  }

  public Command getAutonomousCommand() {
     return new PathPlannerAuto("New Auto");
  }

  public Command getFollowTestPathCommand() {
    return null;
  }

  public Command getOnTheFlyPathComamand() {
   return null;
  }

  public Command getNavigationGridDemoPathCommand() {
    return null;
  }

  public Command goToTargetPosition(double x, double y, double rotationDegrees) {
    return null;
  }

  public Command goToAutoTargetPosition() {
   return null;
  }

  public Command getSwerveDriveCommand() {
    return new DrivetrainDefaultCommand(
        m_drivetrain, m_driverController, m_vision);

  }

public void moveArmToGroundPickup() {
    new MoveArmAndShooterToPosition(m_shooter, m_arm, 5, 50);
}

}  
