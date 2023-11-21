package io.mosip.registration.controller.reg;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.dto.UiSchemaDTO;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_UIN_UPDATE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

/**
 * UpdateUINController Class.
 * 
 * @author Mahesh Kumar
 *
 */
@Controller
public class UpdateUINController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(UpdateUINController.class);

	@Autowired
	private RegistrationController registrationController;

	@FXML
	private TextField uinId;

	@FXML
	private Button backBtn;
	@FXML
	private ImageView backImageView;

	@Autowired
	private UinValidator<String> uinValidatorImpl;

	@Autowired
	private VidValidator<String> vidValidatorImpl;

	@Value("${mosip.kernel.vid.length:-1}")
	private int vidLength;

	@Autowired
	Validations validation;

	@FXML
	FlowPane parentFlowPane;

	private ObservableList<Node> parentFlow;

	private HashMap<String, Object> checkBoxKeeper;

	private Map<String,String> checkBoxMap;

	private Map<String, List<UiSchemaDTO>> groupedMap;

	private FXUtils fxUtils;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		fxUtils = FXUtils.getInstance();
		checkBoxKeeper = new HashMap<>();
		Map<String, UiSchemaDTO> schemaMap = getValidationMap();
		checkBoxMap = new HashMap<>();
		checkBoxMap.put("FullName",RegistrationUIConstants.UPDATE_GROUP_FullName);
		checkBoxMap.put("DateOfBirth",RegistrationUIConstants.UPDATE_GROUP_DateofBirth);
		checkBoxMap.put("placeofbirthLoc",RegistrationUIConstants.UPDATE_GROUP_PlaceofBirth);
		checkBoxMap.put("ResidenceStatus",RegistrationUIConstants.UPDATE_GROUP_ResidentStatus);
		checkBoxMap.put("PermanentAddress",RegistrationUIConstants.UPDATE_GROUP_PermanentAddress);
		checkBoxMap.put("PresentAddress",RegistrationUIConstants.UPDATE_GROUP_PresentAddress);
		checkBoxMap.put("Documents",RegistrationUIConstants.UPDATE_GROUP_Document);
		checkBoxMap.put("IntroducerDetails",RegistrationUIConstants.UPDATE_GROUP_IntroducerDetails);
		checkBoxMap.put("GuardianDetails",RegistrationUIConstants.UPDATE_GROUP_GuardianDetails);
		checkBoxMap.put("Mode of Calim",RegistrationUIConstants.UPDATE_GROUP_ModeofClaim);
		checkBoxMap.put("Email",RegistrationUIConstants.UPDATE_GROUP_Email);
		checkBoxMap.put("Phone",RegistrationUIConstants.UPDATE_GROUP_Phone);


		groupedMap = schemaMap.values().stream().filter(field -> field.getGroup() != null && field.isInputRequired())
				.collect(Collectors.groupingBy(UiSchemaDTO::getGroup));
		String[] removeCheckboxList = RegistrationUIConstants.DISABLED_CHECKBOX_GROUPS.split(":");
		for (String list:
				removeCheckboxList) {
			groupedMap.remove(list);
		}


		parentFlow = parentFlowPane.getChildren();
		groupedMap.forEach((groupName, list) -> {
			GridPane checkBox = addCheckBox(groupName);
			if (checkBox != null) {
				parentFlow.add(checkBox);
			}
		});

		try {
			Image backInWhite = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK_FOCUSED));
			Image backImage = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK));

			backBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
				if (newValue) {
					backImageView.setImage(backInWhite);
				} else {
					backImageView.setImage(backImage);
				}
			});
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	private GridPane addCheckBox(String groupName) {
		CheckBox checkBox;
		if(checkBoxMap.containsKey(groupName)){
			checkBox = new CheckBox(checkBoxMap.get(groupName));
		}else{
			checkBox = new CheckBox(groupName);
		}


		checkBox.getStyleClass().add(RegistrationConstants.updateUinCheckBox);
		fxUtils.listenOnSelectedCheckBox(checkBox);
		checkBoxKeeper.put(groupName, checkBox);

		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(200);
		gridPane.setPrefHeight(40);

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(85);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);

		ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
		RowConstraints rowConstraint1 = new RowConstraints();
		columnConstraint1.setPercentWidth(20);
		RowConstraints rowConstraint2 = new RowConstraints();
		columnConstraint1.setPercentWidth(60);
		RowConstraints rowConstraint3 = new RowConstraints();
		columnConstraint1.setPercentWidth(20);
		rowConstraints.addAll(rowConstraint1, rowConstraint2, rowConstraint3);

		gridPane.add(checkBox, 1, 1);

		return gridPane;
	}

	/**
	 * Submitting for UIN update after selecting the required fields.
	 *
	 * @param event the event
	 */
	@FXML
	public void submitUINUpdate(ActionEvent event) {
		LOGGER.info(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID, "Updating UIN details");
		try {
			if (StringUtils.isEmpty(uinId.getText())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UPDATE_VID_ENTER_UIN_ALERT);
			} else {
				Map<String, UiSchemaDTO> selectedFields = new HashMap<String, UiSchemaDTO>();
				List<String> selectedFieldGroups = new ArrayList<String>();
				for (String key : checkBoxKeeper.keySet()) {
					if (((CheckBox) checkBoxKeeper.get(key)).isSelected()) {
						selectedFieldGroups.add(key);
						for (UiSchemaDTO field : groupedMap.get(key)) {
							selectedFields.put(field.getId(), field);
						}
					}
				}


				LOGGER.debug(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID,
						"selectedFieldGroups size : " + selectedFieldGroups.size());
				LOGGER.debug(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID,
						"selectedFields size : " + selectedFields.size());
//				if (uinValidatorImpl.validateId(uinId.getText()) && !selectedFields.isEmpty()) {
//					System.out.println("uin condition check : "+uinId.getText());
//					registrationController.init(uinId.getText(), checkBoxKeeper, selectedFields, selectedFieldGroups);
//					Parent createRoot = BaseController.load(
//							getClass().getResource(RegistrationConstants.CREATE_PACKET_PAGE),
//							applicationContext.getApplicationLanguageBundle());
//
//					getScene(createRoot).setRoot(createRoot);
//				} else
				if (vidValidatorImpl.validateId(uinId.getText()) && !selectedFields.isEmpty()) {
					System.out.println("vid condition check : "+uinId.getText());

					registrationController.init(uinId.getText(), checkBoxKeeper, selectedFields, selectedFieldGroups);
					Parent createRoot = BaseController.load(
							getClass().getResource(RegistrationConstants.CREATE_PACKET_PAGE),
							applicationContext.getApplicationLanguageBundle());

					getScene(createRoot).setRoot(createRoot);
				}else {
//					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UPDATE_UIN_SELECTION_ALERT);
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UPDATE_VID_SELECTION_ALERT);
				}
//				else if (uinValidatorImpl.validateId(uinId.getText()) && !selectedFields.isEmpty()) {
//					registrationController.init(uinId.getText(), checkBoxKeeper, selectedFields, selectedFieldGroups);
//					Parent createRoot = BaseController.load(
//							getClass().getResource(RegistrationConstants.CREATE_PACKET_PAGE),
//							applicationContext.getApplicationLanguageBundle());
//
//					getScene(createRoot).setRoot(createRoot);
//				}

			}
		} catch (InvalidIDException invalidIdException) {
			LOGGER.error(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID,
					invalidIdException.getMessage() + ExceptionUtils.getStackTrace(invalidIdException));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UPDATE_VID_VALIDATION_ALERT);
		} catch (IOException ioException) {
			LOGGER.error(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_REG_PAGE);
		}
	}
}
