package com.example.todoapp.fragments.list

import android.app.AlertDialog
import android.os.Bundle
import android.view.*

import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.example.todoapp.R
import com.example.todoapp.data.models.ToDoData
import com.example.todoapp.data.viewmodel.ToDoViewModel
import com.example.todoapp.databinding.FragmentListBinding
import com.example.todoapp.fragments.SharedViewModel
import com.example.todoapp.fragments.list.adpater.ListAdapter
import com.example.todoapp.utils.hideKeyboard
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator

class ListFragment : Fragment(), SearchView.OnQueryTextListener {

    private val mToDoViewModel: ToDoViewModel by viewModels()
    private val mSharedViewModel: SharedViewModel by viewModels()

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val adpater = ListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Data binding
        _binding = FragmentListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.mSharedViewModel = mSharedViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.list_fragment_menu, menu)
                       val search = menu.findItem(R.id.menu_search)
                       val searchView = search.actionView as? SearchView
                       searchView?.isSubmitButtonEnabled = true
                       searchView?.setOnQueryTextListener(this@ListFragment)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_delete_all -> confirmRemoval()
                      R.id.menu_priority_high ->
                          mToDoViewModel.sortByHighPriority.observe(viewLifecycleOwner) {
                              adpater.setData(it)
                          }
                      R.id.menu_priority_low ->
                          mToDoViewModel.sortByLowPriority.observe(viewLifecycleOwner) {
                              adpater.setData(it)
                          }
                    android.R.id.home -> requireActivity().onBackPressed()
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // observing a livedata
        mToDoViewModel.getAllData.observe(viewLifecycleOwner, Observer { data ->
            mSharedViewModel.checkIfDatabaseEmpty(data)
            adpater.setData(data)
        })
        // setupRecyclerview
        setUpRecyclerview()

        // hideKeyboard
        hideKeyboard(requireActivity())

    }

    private fun setUpRecyclerview() {
        binding.recyclerView.adapter = adpater
        binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerView.itemAnimator = SlideInUpAnimator().apply {
            addDuration = 300
        }
        swipeToDelete(binding.recyclerView)

    }

    private fun restoreDeletedData(view: View, deletedItem: ToDoData) {
        val snackbar =
            Snackbar.make(view, "DeletedItem '${deletedItem.title}'", Snackbar.LENGTH_LONG)

        snackbar.setAction("Undo") {
            mToDoViewModel.insertData(deletedItem)

        }
        snackbar.show()

    }

    private fun swipeToDelete(recyclerView: RecyclerView) {
        val swipeToDeleteCallback = object : SwipeToDelete() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val itemToDelete = adpater.dataList[viewHolder.adapterPosition]
                mToDoViewModel.deleteData(itemToDelete)
                adpater.notifyItemRemoved(viewHolder.adapterPosition)
                // restore Deleted Item
                restoreDeletedData(viewHolder.itemView, itemToDelete)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun confirmRemoval() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setPositiveButton("Yes") { _, _ ->
            mToDoViewModel.deleteAll()
            Toast.makeText(requireContext(), "Deleted Successfully", Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton("No") { _, _ ->
        }
        builder.setTitle("Delete everything")
        builder.setMessage("Are you sure you want to delete All entries")
        builder.create().show()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onQueryTextSubmit(query: String?): Boolean {

        if (query != null){
            searchInDatabase(query)
        }
        return true
    }

    private fun searchInDatabase(query: String) {
        val searchQuery = "%$query%"
        mToDoViewModel.searchDatabase(searchQuery).observe(this, Observer { list->
            list?.let {
                adpater.setData(it)
            }
        })
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null){
            searchInDatabase(query)
        }
        return true
    }
}