import React, { Component } from 'react';
import { AppState, View, Text, StyleSheet, TouchableOpacity, Platform, Modal, TouchableWithoutFeedback, TouchableHighlight, Image } from 'react-native';
import MapView from 'react-native-maps';
import Dimensions from 'Dimensions';
const {width, height} = Dimensions.get('window');
import MainOverlayControl from './components/MainOverlayControl';
import ServerTools from '../utils/ServerTools';
import Db_Helper_User from '../utils/Db_Helper_User';
import Db_Helper_Data from '../utils/Db_Helper_Data';

const LATITUDE_DELTA = 0.0001;
const LONGITUDE_DELTA = 0.0001;

class MainScreen extends Component{
	static navigatorStyle = {
	 	screenBackgroundColor: '#2f4858',
	  statusBarColor: '#2f4858',
	  navBarBackgroundColor: '#2f4858',
	  navBarTextColor: 'white',
	  navBarButtonColor: 'white',
	  navBarComponentAlignment: 'fill',
	  navBarCustomView: 'vt.CustomTopBar'
	};
	static navigatorButtons = {
		rightButtons: [
			{
				id: 'settings',
				icon: require('../res/ic_settings_icon.png')
			}
		],
		leftButtons: [
			{
				id: 'profile',
				icon: require('../res/ic_profile_icon.png')
			}
		]
	};

	constructor(props) {
	 super(props);
	 map = null;
	 
	 this.state = {
	 	modalVisible: false,
	 	region: {
	 		latitude: 34.057712,
			longitude: -117.820757,
			latitudeDelta: 0.0122,
			longitudeDelta: 0.0121,
	 	},
	 	ready: false,
	 	appState: AppState.currentState,
	 	markers: [],
	 };

	 this.AttemptLogOff = this.AttemptLogOff.bind(this);
	 this.pushBugReportScreen = this.pushBugReportScreen.bind(this);
	 this.props.navigator.setOnNavigatorEvent(this.onNavigatorEvent.bind(this));
	 this.getCurrentPosition = this.getCurrentPosition.bind(this);
	 this.addMarkers = this.addMarkers.bind(this);
	 this.onMarkerPress = this.onMarkerPress.bind(this);
	}

	componentDidMount() {
		this.setState({ready: true});
		this.updateServerPosition();
		AppState.addEventListener('change', this._handleAppStateChange);
	}

	componentWillUnmount() {
    AppState.removeEventListener('change', this._handleAppStateChange);
  }

	//Handler which deals with setting a timer when app is left from focus to background
	//will logout after 30 seconds of inactivity
  _handleAppStateChange = (nextAppState) => {
  	if(this.state.appState.match(/active/) && nextAppState === 'background'){
  		console.log('App has been backgrounded');
  		this.timer = setTimeout(() => this.AttemptLogOff(), 1000*30) // set a timer to auto log off after 30 seconds inactivity
  	}

    if (this.state.appState.match(/inactive|background/) && nextAppState === 'active') {
      console.log('App has come to the foreground!')
      console.log('Canceling timer test');
      clearTimeout(this.timer);
    }
    this.setState({appState: nextAppState});
  }

	setRegion(region) {
		if(this.state.ready) {
			setTimeout(() => this.map.animateToRegion(region), 10);
		}
	}

	async updateLocation(position){
		let sessionData = await Db_Helper_User.getSessionData();
		let response = await ServerTools.updateLocation({'token_user': sessionData.token_user, 'user_id': sessionData.user_id, 'action': 'updateLocation', 'lat': position.coords.latitude, 'lng': position.coords.longitude});
		console.log(response);
	}

	updateServerPosition(){
		navigator.geolocation.getCurrentPosition(
			(position) => this.updateLocation(position),
			(error) => console.log('error getting current position'),
		);
	}

	getCurrentPosition() {
		try{
			navigator.geolocation.getCurrentPosition(
				(position) => {
					const region = {
						latitude: position.coords.latitude,
						longitude: position.coords.longitude,
						latitudeDelta: LATITUDE_DELTA,
						longitudeDelta: LONGITUDE_DELTA,
					};
					this.setRegion(region);
				},
				(error) => console.log("error getting current position"),
			);
		} catch(e) {
			console.log("error trying geolocation")
		}
	}

	showMenu(toggle) {
		this.setState({modalVisible: toggle});
	}

	pushBugReportScreen() {
		this.showMenu(false);
		this.props.navigator.push({
			screen: 'vt.BugReportScreen',
			title: 'Give us Feedback',
			animationType: 'fade',
			backButtonTitle: '',
		});
	}

	async AttemptLogOff() {
		this.showMenu(false);
		let sessionData = await Db_Helper_User.getSessionData();
		let response = await ServerTools.logoff(sessionData);
		if(response != null){
			if (response.code==1){
				this.props.navigator.resetTo({
					screen: 'vt.LoginScreen',
					animated: true,
					animationType: 'fade',
					passProps: {fromMain: true}
				});
			}
		}
	}

	AttemptMatch = () => {
		this.props.navigator.push({
			screen: 'vt.WaitingScreen',
			backButtonHidden: true,
		});
	}

	onNavigatorEvent(event) {
		if(event.type == 'NavBarButtonPress'){
			if(event.id == 'profile'){
				this.props.navigator.push({
					screen: 'vt.ProfileScreen',
					title: 'Profile',
					backButtonHidden: false,
					backButtonTitle: '',
					animated: true
				});
			}
			if(event.id == 'settings'){
				this.showMenu(true);
			}
		}
	}

	addMarkers(markers)  {
		this.setState({markers: markers});
	}

	onMarkerPress(e){
		const region = {
			latitude: e.nativeEvent.coordinate.latitude,
			longitude: e.nativeEvent.coordinate.longitude,
			latitudeDelta: 0.002,
			longitudeDelta: 0.004,
		};
		this.map.animateToRegion(region);
		this.mainoverlay.setParkingSelectionTo(e.nativeEvent.id);
	}

	onRegionChange(region){
		// console.log(region);
		this.setState({region: region});
	}

	render() {
		let markers=null;
		if(this.state.markers){
			markers = this.state.markers.map(marker => (
				<MapView.Marker
					title={marker.title}
					coordinate={marker}
					key={marker.key}
					identifier={marker.title}
					onPress={this.onMarkerPress}
					image={require('../res/other_marker.png')}
				/>
			));
		}

		return (
			<View style={styles.container}>
				<View style={styles.mapContainer}>
						<MapView 
							style={styles.map}
							ref={ref => {this.map = ref}}
							showsUserLocation={true}
							showsMyLocationButton={false}
							loadingEnabled={false}
							initialRegion={this.state.region}
							onRegionChange={region => this.onRegionChange(region)}
						>
							{markers}
						</MapView>
					</View>
				<Modal 
					onRequestClose={() => this.showMenu(false)}
					animationType='fade'
					transparent={true}
					visible={this.state.modalVisible}>
					<TouchableWithoutFeedback onPress={() => this.showMenu(false)}>
						<View style={styles.menuContainer}>
							<View style={{backgroundColor: '#2F2F2F'}}>
								<TouchableHighlight onPress={this.pushBugReportScreen} underlayColor='white'>
						    	<View style={styles.menuButton}>
						    		<Text style={styles.menubuttonText}>Suggestions/Report Bug</Text>
						    	</View>
						    </TouchableHighlight>
								<TouchableHighlight onPress={this.AttemptLogOff} underlayColor='white'>
						    	<View style={styles.menuButton}>
						    		<Text style={styles.menubuttonText}>Log Off</Text>
						    	</View>
						    </TouchableHighlight>
							</View>
						</View>
					</TouchableWithoutFeedback>
				</Modal>
				
				<MainOverlayControl 
					ref={ref => {this.mainoverlay = ref}}
					addMarkers={this.addMarkers}
					map={this.map}
					navigator={this.props.navigator}
					getCurrentPosition={this.getCurrentPosition}
					region={this.state.region}
				/>
			</View>
		);
	}
}

const styles = StyleSheet.create({
	container: {
		// backgroundColor: 'red'
	},
	mapContainer: {
    height: height,
    width: width
  },
  map: {
    ...StyleSheet.absoluteFillObject
  },
  menuContainer: {
  	flex:1,
  	flexDirection: 'column',
  	justifyContent: 'flex-start',
  	alignItems: 'flex-end',
  	marginTop: 20,
  	marginRight: 5,
  },
  menuButton: {
    alignItems: 'flex-start',
    backgroundColor: '#2F2F2F',
  },
  menubuttonText: {
  	fontSize: 16,
		padding: 14,
    color: 'white'
  },
});

export default MainScreen