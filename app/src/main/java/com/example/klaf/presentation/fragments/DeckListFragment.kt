package com.example.klaf.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.compose.navArgument
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.klaf.R
import com.example.klaf.databinding.FragmentDeckListBinding
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.presentation.adapters.DeckAdapter
import java.util.zip.Inflater

class DeckListFragment : Fragment() {
    private var _binding: FragmentDeckListBinding? = null
    private val binding get() = _binding!!
    private var recyclerViewDecks: RecyclerView? = null
    private val decks = ArrayList<Deck>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDeckListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->

            val navController = findNavController()
            val adapter = DeckAdapter()
            adapter.testDecks = decks
            recyclerViewDecks = binding.recyclerviewDecks
            recyclerViewDecks?.let { recycler ->
                recycler.layoutManager = LinearLayoutManager(activity.applicationContext)
                recycler.adapter = adapter
                adapter.onClick = {
                    navController.navigate(R.id.action_deckListFragment_to_repeatFragment)
                    Toast.makeText(context, "onClick", Toast.LENGTH_SHORT).show()
                }
                adapter.onLongClick = { TODO("Not yet implemented") }
            }

            binding.button.setOnClickListener {
                navController.navigate(R.id.action_deckListFragment_to_repeatFragment)
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}