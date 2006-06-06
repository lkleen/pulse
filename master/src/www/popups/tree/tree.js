/**
 * Initialise the tree control.
 *
 */
function init ()
{
    var anchorId = getConfig().anchor;

    // need to find a way to extract the id of the root tree node from this file.
    var anchorDiv = document.getElementById(anchorId);

    // add loading place holder.
    var ul = document.createElement("ul");
    ul.appendChild(createNewNode({"file":"Loading...", "type":"loading", "uid":""}));
    anchorDiv.appendChild(ul);

    // trigger an initial load.
    requestUpdate("");
}

function load(event)
{
    var currentTarget = getCurrentTarget(event);

    if (this == currentTarget)
    {
        // WARNING: using innerHTML directly clears out any event handlers.
        // insert another level of the tree. <ul>loading...</ul>

        var ul = document.createElement("ul");
        ul.appendChild(createNewNode({"file":"Loading...", "type":"loading", "uid":""}));

        currentTarget.appendChild(ul);

        // now we change the onclick handler so that it handles toggling instead of loading.
        currentTarget.onclick = toggle;

        // open current target.
        Element.removeClassName(currentTarget, "folder");
        Element.addClassName(currentTarget, "openfolder");

        // send off the xml http request.
        requestUpdate(currentTarget.id);
    }
}

/**
 * Simple encapsulation of the configuration object. This should simplify the fixing how
 * the configuration details are passed around.
 */
function getConfig()
{
    return window.myTree;
}

function requestUpdate(id)
{
    var url = getConfig().url;

    var ajax = new Ajax.Request(
        url,
        {
            method: 'get',
            onComplete: updateFlat,
            onFailure: handleFailure,
            onException: handleException,
            parameters:"uid=" + id
        }
    );
}

/**
 * A callback handler to process the response from a 'listing' request to the
 * server. This handler constructs a full navigable directory tree.
 *
 * This handler expected a response in the format:
 *
 *    listing:  {file, type, id}
 *    path: 'parent id'
 *
 * where
 *    file - is the name of the file
 *    type - is the type of the file (folder, file, txt etc)
 *    id - is the unique identifier for this file.
 *
 * and
 *
 *    path - represents the unique id of the parent.
 */
function updateTree(originalRequest)
{
    var jsonText = originalRequest.responseText;
    var jsonObj = eval("(" + jsonText + ")");
    var listing = jsonObj.listing;

    var target = document.getElementById(jsonObj.uid);
    if (!target)
    {
        target = document.getElementById(getConfig().anchor);
    }

    // clean the "loading..." out of the list.
    var ul = locateFirstChild(target, "UL");
    removeAllChildren(ul);

    // add the '.' directory so that it can be selected. However, we do not want it to be
    // reloaded since it is a special case that clears out all existing content...
    if (!jsonObj.uid)
    {
        var thisDirectory = createNewNode({"file":".", "type":"root", "uid":""});
        ul.appendChild(thisDirectory);
    }

    for (var i = 0; i < listing.length; i++)
    {
        var listItem = createNewNode(listing[i]);
        ul.appendChild(listItem);
    }
}

function updateFlat(originalRequest)
{
    var folder = document.getElementById(getConfig().anchor);

    var jsonText = originalRequest.responseText;
    var jsonObj = eval("(" + jsonText + ")");
    var listing = jsonObj.listing;

    removeChild(folder);
    clearSelection();

    var ul = document.createElement("ul");
    folder.appendChild(ul);

    // add the links to the current directory.
    var path = jsonObj.uid;
    if (!path)
    {
        path = "";
    }
    var thisDirectory = createNewNode({"file":".", "type":"folder", "uid":path});
    ul.appendChild(thisDirectory);

    if (jsonObj.puid)
    {
        var puid = jsonObj.puid;
        var parentDirectory = createNewNode({"file":"..", "type":"folder", "uid":puid});
        ul.appendChild(parentDirectory);
    }

    for (var i = 0; i < listing.length; i++)
    {
        var listItem = createNewNode(listing[i]);
        ul.appendChild(listItem);
    }

    // display path if it is available.
    getConfig().displayPath = jsonObj.displayPath;

    var currentPathDisplay = document.getElementById('path');
    if (currentPathDisplay)
    {
        removeAllChildren(currentPathDisplay);
        // update the current node status.
        if (jsonObj.displayPath)
        {
            currentPathDisplay.appendChild(document.createTextNode(jsonObj.displayPath));
        }
    }
}

/**
 * Create a new node.
 *
 *    data: an associative array with fields id, file and type.
 *
 */
function createNewNode(data)
{
    var node = document.createElement("li");
    node.appendChild(document.createTextNode(data.file));
    node.setAttribute("id", data.uid);
    Element.addClassName(node, data.type);
    if (data.type == "folder")
    {
        node.ondblclick = load;
        node.onclick = select;
    }
    else if (data.type == "loading")
    {
        // do nothing here..
    }
    else
    {
        node.onclick = select;
    }
    return node;
}

/**
 * Select the element that is the target of this event.
 *
 * Selecting an element will add the 'selected' class to its list of classes.
 * Only a single element can be selected at a time.
 */
function select(event)
{
    var currentTarget = getCurrentTarget(event);
    if (this == currentTarget)
    {
        // locate the selected class.
        clearSelection();

        Element.addClassName(currentTarget, "selected");

        if (Element.hasClassName(currentTarget, "folder"))
        {
            return;
        }

        getConfig().selectedValue = extractText(currentTarget);

        // update selected display.
        // - what is the currently selected name?
        var selectedDisplay = document.getElementById('selected');
        if (selectedDisplay)
        {
            removeAllChildren(selectedDisplay);

            //selectedDisplay.appendChild(document.createTextNode(currentTarget.id));
            selectedDisplay.value = extractText(currentTarget);
        }
    }
}

function clearSelection()
{
    var selectedNodes = document.getElementsByClassName("selected");

    // remove 'selected' from the list of classes.
    if (selectedNodes)
    {
        for (var i = 0; i < selectedNodes.length; i++)
        {
            var node = selectedNodes[i];
            Element.removeClassName(node, "selected");
        }
    }
    clearBrowserTextSelection();
}

function extractText(element)
{
    return element.innerHTML;
}

/**
 * Toggle the state of the element that is the target of this event.
 *
 * NOTE: It only makes sense for the target node to represent a 'folder'.
 */
function toggle(event)
{
    var currentTarget = getCurrentTarget(event);
    if (this == currentTarget)
    {
        var node = this;
        var ul = locateFirstChild(node, "UL");
        Element.toggle(ul);
        if (Element.visible(ul))
        {
            replaceClassName(node, "folder", "openfolder");
        }
        else
        {
            replaceClassName(node, "openfolder", "folder");
        }
    }
}

function replaceClassName(element, oldClassName, newClassName)
{
    Element.removeClassName(element, oldClassName);
    Element.addClassName(element, newClassName);
}

function removeChild(element)
{
    var children = element.childNodes;
    for (var j = 0; j < children.length; j++)
    {
        var child = children[j];
        if (child.nodeType == 1 && (child.tagName.toUpperCase() == "UL"))
        {
            Element.remove(child);
        }
    }
}

/**
 * Remove all of the child nodes from the specified element.
 */
function removeAllChildren(element)
{
    var children = $A(element.childNodes);
    children.each(function(child)
    {
        // remove the child element from the document.
        Element.remove(child);
    });
}

function getCurrentTarget(event)
{
    return Event.element(getCurrentEvent(event));
}

function getCurrentEvent(event)
{
    return event || window.event;
}

function clearBrowserTextSelection()
{
    if (document.selection)
    {
        document.selection.empty();
    }
}

/**
 * Locate and return the first child of the specified element that has a nodeName property
 * that matches the specified nodeName.
 *
 */
function locateFirstChild(elem, nodeName)
{
    nodeName = nodeName.toUpperCase();

    // add Enumerable to childNodes
    var children = $A(elem.childNodes);

    var res = null;
    children.each(function(child) {
        var name = child.nodeName;
        if (name && name.toUpperCase() == nodeName)
        {
            res = child;
            return;
        }
    });
    return res;
}

function debug(element)
{
    var debugging = "";
    var i = 0;
    for (var propName in element)
    {
        var sep = "\n"
        if (i < 5)
        {
            sep = ", "
        }
        else
        {
            i = 0;
        }
        debugging += sep + propName;
        i++;
    }
    alert(debugging);
}

/**
 * Basic failure handler.
 *
 */
function handleFailure(resp)
{
    alert("onFailure");
}

/**
 * Basic exception handler.
 *
 */
function handleException(resp, e)
{
    alert("onException: " + e);
}