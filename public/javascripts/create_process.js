var app = angular.module('CreateProcessApp', []);

app.controller('ProcessController', ['$scope', '$http', '$window', function($scope, $http, $window){
	
	$scope.process = [];
	$scope.activities = [];
	$scope.node = [];
	$scope.count = [{"index":0}];
	
	
	// get configuration information of all nodes
	$http.get('/app/nodes/all')
	.success(function(responseData){
		$scope.nodes = responseData;
	}).error(function(errorData){
		$scope.error = errorData;
	});
	
	
	// update the count variable when the Next Action button is clicked
	$scope.nextAction = function() {
		$scope.count.push({"index":$scope.count.length});
	}


	// update the list of activities and input/output variables
	// for the given node
	$scope.updateActivities = function(index) {
		$http.get('/app/node/' + $scope.node[index].nodeId)
		.success(function(responseData){
			if(index==0) {
				$scope.activities[index] = responseData.triggers;
				$scope.process[index] = {
						"node":$scope.node[index].nodeId, 
						"name":$scope.node[index].nodeName, 
						"logo":$scope.node[index].nodeLogo, 
						"description":$scope.node[index].nodeDescription, 
						"data":responseData.triggers[0]
				};
			} else {
				$scope.activities[index] = responseData.actions;
				$scope.process[index] = {
						"node":$scope.node[index].nodeId, 
						"name":$scope.node[index].nodeName, 
						"logo":$scope.node[index].nodeLogo, 
						"description":$scope.node[index].nodeDescription, 
						"data":responseData.actions[0]
				};
			}

			// update the form view
			$scope.updateFormView(index);
			
		}).error(function(errorData){
			$scope.error = errorData;
		});
	}
		
	
	// get the service data (used for input dropdowns) for the given node
	$scope.getData = function(data, node, source) {
		data.showWorking = true;
		data.disableServiceButton = true;
		$http.get('/app/node/' + node + '/' + source)
		.success(function(responseData){
			data.data = responseData;
			data.showData = true;
			data.showWorking = false;
			data.disableServiceButton = false;
		}).error(function(errorData){
			$scope.error = errorData;
			data.disableServiceButton = false;
			data.showWorking = false;
		});
	}
	
	
	$scope.saveProcess = function() {
		$('#processData').val(JSON.stringify($scope.process));
		$('#triggerNode').val($scope.process[0].node);
		$('#save_process_form').submit();
	}
	

	// add text to the input box on selecting 
	// the output value from previous node
	$scope.addText = function(model, nodeIndex, value) {
		if(model.value)
			model.value = model.value + ' ##'+nodeIndex+'.'+value+'##';
		else
			model.value = '##'+nodeIndex+'.'+value+'##';
			
	}

	$scope.openAuthorizeWindow = function(node) {
		$scope.currentNode = node;
		$window.open(node.authURL, '_new_');
	}
	
	$scope.authorizeCurrentNode = function() {
		$scope.currentNode.authorized = true;
	}
	
	// update the form view - removes all next actions and resets input for the current action
	// used when a user changes the node or activity (for all except last action)
	$scope.updateFormView = function(index) {
		var length = $scope.count.length;
		var pos = index + 1;
		if(pos < length) {
			$scope.count.splice(pos, length - pos);
			$scope.process.splice(pos, length - pos);
			$scope.node.splice(pos, length - pos);
		}
	}

}]);

/**
Callback function thats gets called by the subwindow for oauth authorization
**/
function oauthCallback() {
	angular.element($('#ProcessController')).scope().$apply(function(scope){
		scope.authorizeCurrentNode();
	});
}
