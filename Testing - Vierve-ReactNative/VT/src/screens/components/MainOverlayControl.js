import React, {Component} from 'react';
import {BackHandler, View} from 'react-native';

import CollegeOverlay from './CollegeOverlay';
import ParkingOverlay from './ParkingOverlay';
import PickupOverlay from './PickupOverlay';

class MainOverlayControl extends Component {
	constructor(props) {
		super(props);
		this.handlePark = this.handlePark.bind(this);
		this.handleRide = this.handleRide.bind(this);
		this.handleParkingSet = this.handleParkingSet.bind(this);
		this.handleParkingBack = this.handleParkingBack.bind(this);
		this.handlePickupSet = this.handlePickupSet.bind(this);
		this.handlePickupBack = this.handlePickupBack.bind(this);
		this.state= {
			screen : 0,
			riding: false,
		};
	}

	componentDidMount() {
		BackHandler.addEventListener('androidBackPress', function() {
			console.log("hello test backpressing")
		});
	}

	handlePark(){
		// this.toggleNavBar('hidden');
		this.setState({screen:1, riding: false});
	}

	handleRide(){
		// this.toggleNavBar('hidden');
		this.setState({screen:1, riding: true});
	}

	handleParkingSet(){
		if (this.state.riding){
			this.setState({screen:2});
		}
		else{
			this.props.navigator.push({
				screen: 'vt.WaitingScreen',
				backButtonHidden: true,
			});
		}
	}

	handlePickupSet() {
		this.props.navigator.push({
				screen: 'vt.WaitingScreen',
				backButtonHidden: true,
		});
	}

	handleParkingBack(){
		this.toggleNavBar('shown');
		this.setState({screen: 0});
	}

	handlePickupBack(){
		this.setState({screen: 1});
	}

	toggleNavBar(value) {
		this.props.navigator.toggleNavBar({
  		to: value, // required, 'hidden' = hide navigation bar, 'shown' = show navigation bar
  		animated: true // does the toggle have transition animation or does it happen immediately (optional). By default animated: true
		});
	}

	render(){
		const screen = this.state.screen;
		let overlay = null;

		if(screen == 0){
			overlay = <CollegeOverlay onPark={this.handlePark} onRide={this.handleRide} onGetPosition={this.props.getCurrentPosition}/>;
		} else if(screen==1){
			overlay = <ParkingOverlay onParkingSet={this.handleParkingSet} onBackPress={this.handleParkingBack} onGetPosition={this.props.getCurrentPosition}/>;
		} else {
			overlay = <PickupOverlay onPickupSet={this.handlePickupSet} onBackPress={this.handlePickupBack} onGetPosition={this.props.getCurrentPosition}/>;
		}

		return(
			overlay
		);
	}
}

export default MainOverlayControl